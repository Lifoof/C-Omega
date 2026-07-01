package com.health.report.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Cell;
import com.health.report.domain.AgeThreshold;
import com.health.report.dto.collection.ThresholdExcelData;
import com.health.report.service.IAgeThresholdService;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 终极版动态宽表导入监听器
 * 全版本EasyExcel兼容、无API报错、无泛型不匹配、完美动态列解析
 * Excel格式：列0=年龄 列1=性别  列2开始全为 xxx_min/_max 动态指标列
 */
public class DynamicThresholdImportListener extends AnalysisEventListener<Map<Integer, String>> {

    private static final int BATCH_SIZE = 200;
    private final List<AgeThreshold> batchDataList = new ArrayList<>();

    private final IAgeThresholdService thresholdService;
    // 表头中文名 -> 数据库fieldCode 编码映射
    private final Map<String, String> nameToCodeMap;

    // 表头解析结果：指标纯名称 -> [min列下标, max列下标]
    private Map<String, int[]> indicatorColumnMap;
    // 标记：表头是否已经解析完成
    private boolean headParsed = false;

    // 固定列下标
    private static final int AGE_COL = 0;
    private static final int GENDER_COL = 1;

    public DynamicThresholdImportListener(IAgeThresholdService thresholdService, Map<String, String> nameToCodeMap) {
        this.thresholdService = thresholdService;
        this.nameToCodeMap = nameToCodeMap;
        this.indicatorColumnMap = new HashMap<>();
    }

    // ====================== 所有行解析总入口（原生基础方法，全版本100%兼容） ======================
    @Override
    public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
        // 【第一次执行：解析表头行】
        if (!headParsed) {
            parseHead(rowData);
            headParsed = true;
            return; // 表头行不入库，直接结束
        }

        // 【后续所有行：解析真实数据行】
        // 1. 读取固定列：年龄、性别
        String ageStr = rowData.get(AGE_COL);
        String genderStr = rowData.get(GENDER_COL);
        if (!StringUtils.hasText(ageStr) || !StringUtils.hasText(genderStr)) return;

        Integer age, gender;
        try {
            age = Integer.parseInt(ageStr.trim());
            gender = parseGender(genderStr.trim());
        } catch (Exception e) {
            return;
        }

        if (gender == null) return;

        // 2. 遍历所有解析完成的动态指标，读取min/max数值
        for (Map.Entry<String, int[]> entry : indicatorColumnMap.entrySet()) {
            String indicatorName = entry.getKey();
            int[] colPair = entry.getValue();
            int minCol = colPair[0];
            int maxCol = colPair[1];

            // min/max列下标未完整配对，跳过该指标
            if (minCol <= 1 || maxCol <= 1) continue;

            // 直接根据列下标获取单元格字符串（天生无对象、无API调用、全版本兼容）
            String minStr = rowData.get(minCol);
            String maxStr = rowData.get(maxCol);
            if (!StringUtils.hasText(minStr) || !StringUtils.hasText(maxStr)) continue;

            BigDecimal minVal, maxVal;
            try {
                minVal = new BigDecimal(minStr.trim());
                maxVal = new BigDecimal(maxStr.trim());
            } catch (Exception e) {
                continue;
            }

            // 匹配数据库指标业务编码
            String fieldCode = nameToCodeMap.get(indicatorName);
            if (!StringUtils.hasText(fieldCode)) continue;

            // 封装数据库实体
            AgeThreshold threshold = new AgeThreshold();
            threshold.setAge(age);
            threshold.setGender(gender);
            threshold.setFieldCode(fieldCode);
            threshold.setMinVal(minVal);
            threshold.setMaxVal(maxVal);
            threshold.setCreateTime(new Date());
            threshold.setUpdateTime(new Date());
            batchDataList.add(threshold);
        }

        // 达到批量插入阈值，执行入库
        if (batchDataList.size() >= BATCH_SIZE) {
            batchInsert();
        }
    }

    // ====================== 表头解析核心方法 ======================
    private void parseHead(Map<Integer, String> headRowData) {
        indicatorColumnMap.clear();
        // 遍历全部表头列
        for (Map.Entry<Integer, String> entry : headRowData.entrySet()) {
            int colIndex = entry.getKey();
            String headName = entry.getValue();

            // 跳过前2列固定列：年龄、性别
            if (colIndex == AGE_COL || colIndex == GENDER_COL) continue;
            if (!StringUtils.hasText(headName)) continue;

            // 自动切割 _min / _max 后缀，提取纯指标名称
            boolean isMin = headName.endsWith("_min");
            boolean isMax = headName.endsWith("_max");
            if (!isMin && !isMax) continue;

            String indicatorName = headName.substring(0, headName.length() - 4);
            int[] colPair = indicatorColumnMap.getOrDefault(indicatorName, new int[2]);

            if (isMin) colPair[0] = colIndex;
            else colPair[1] = colIndex;

            indicatorColumnMap.put(indicatorName, colPair);
        }
    }

    /**
     * Excel全部读取完毕，插入剩余缓存数据
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        batchInsert();
        batchDataList.clear();
    }

    /**
     * 调用Service批量幂等插入
     */
    private void batchInsert() {
        if (batchDataList.isEmpty()) return;
        thresholdService.batchInsertWithUpdate(batchDataList);
        batchDataList.clear();
    }

    /**
     * 解析性别字段
     * Excel格式：1=男，0=女
     * 数据库存储：1=男性，2=女性
     */
    private Integer parseGender(String genderStr) {
        if (!StringUtils.hasText(genderStr)) return null;

        String normalized = genderStr.trim();

        // 数字格式：Excel中 1=男，0=女；数据库中 1=男，2=女
        if ("1".equals(normalized) || "1.0".equals(normalized)) {
            return 1; // 男性
        }
        if ("0".equals(normalized) || "0.0".equals(normalized)) {
            return 2; // 女性（Excel的0映射为数据库的2）
        }

        // 中文格式
        if (normalized.contains("男")) {
            return 1; // 男性
        }
        if (normalized.contains("女")) {
            return 2; // 女性
        }

        // 英文格式
        String lower = normalized.toLowerCase();
        if (lower.contains("male") && !lower.contains("female")) {
            return 1; // 男性
        }
        if (lower.contains("female") || lower.contains("women") || lower.contains("woman")) {
            return 2; // 女性
        }

        // 无法识别
        return null;
    }
}
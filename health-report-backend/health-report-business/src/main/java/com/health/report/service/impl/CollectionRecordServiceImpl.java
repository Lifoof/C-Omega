package com.health.report.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.report.common.exception.BusinessException;
import com.health.report.common.exception.ServiceException;
import com.health.report.common.result.ResultCode;
import com.health.report.common.utils.DateUtils;
import com.health.report.common.utils.SecurityUtils;
import com.health.report.domain.*;
import com.health.report.dto.collection.*;
import com.health.report.mapper.*;
import com.health.report.service.ICollectionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采集记录Service业务层处理
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@Service
public class CollectionRecordServiceImpl implements ICollectionRecordService {

    @Autowired
    private CollectionRecordMapper collectionRecordMapper;

    @Autowired
    private CollectionFieldConfigMapper collectionFieldConfigMapper;

    @Autowired
    private MemberInfoMapper memberInfoMapper;

    @Autowired
    private CollectionDataItemMapper collectionDataItemMapper;

    @Override
    public CollectionRecord getById(Long id) {
        CollectionRecord record = collectionRecordMapper.selectById(id);
        if (record == null || record.getDeleted() != null && record.getDeleted() == 1) {
            throw new ServiceException("采集记录不存在");
        }
        // 检查权限
        if (!isAdmin() && !record.getUserId().equals(getCurrentUserId())) {
            throw new ServiceException("无权访问此采集记录");
        }
        return record;
    }

    @Override
    public List<CollectionRecord> page(CollectionRecord record) {
        if (!isAdmin()) {
            record.setUserId(getCurrentUserId());
        }
        return collectionRecordMapper.selectList(record);
    }

    @Override
    @Transactional
    public CollectionRecord create(CollectionRecord record) {
        record.setCollectionNo("COL" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        record.setUserId(getCurrentUserId());
        record.setSourceType(1);
        record.setReportStatus(0);
        record.setFinishStatus(0);
        record.setDeleted(0);
        record.setCreateBy(SecurityUtils.getLoginUser().getUsername());
        record.setCreateTime(DateUtils.getNowDate());
        final int insert = collectionRecordMapper.insert(record);
        if (insert > 0){
            final MemberInfo memberInfo = memberInfoMapper.selectById(record.getMemberId());
            if (memberInfo != null){
                record.setMemberName(memberInfo.getName());
                record.setMemberGender(memberInfo.getGender());
            }
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategoryDataItem(Long collectionId, CategorySaveDTO dto) {
        // 1. 获取前端传的指标JSON：{ bmi:22, height:175... }
        Map<String, Object> fieldData = dto.getFieldData();
        if (CollectionUtils.isEmpty(fieldData)) {
            return;
        }

        String categoryCode = dto.getCategoryCode();

        // 2. 先删除当前采集ID + 分类下的旧指标（覆盖保存）
        collectionDataItemMapper.deleteByCollectionIdAndCategoryCode(collectionId, categoryCode);

        // 3. 查询指标配置（用于判断异常）
        List<CollectionFieldConfig> configList = collectionFieldConfigMapper.selectByCategoryCode(categoryCode);
        Map<String, CollectionFieldConfig> configMap = configList.stream()
                .collect(Collectors.toMap(CollectionFieldConfig::getFieldCode, c -> c));

        // 4：遍历前端JSON → 构建 List
        List<CollectionDataItem> itemList = new ArrayList<>();
        String username = SecurityUtils.getUsername();
        // 1. 先查采集主表，拿到会员性别
        CollectionRecord record = collectionRecordMapper.selectById(collectionId);
        if (record == null) {
            throw new BusinessException("采集记录不存在");
        }
        // 性别：1=女 2=男
        Integer gender = record.getMemberGender();
        for (Map.Entry<String, Object> entry : fieldData.entrySet()) {
            String fieldCode = entry.getKey();
            Object value = entry.getValue();
            if (value == null) continue;

            CollectionFieldConfig config = configMap.get(fieldCode);
            if (config == null) continue;

            CollectionDataItem item = new CollectionDataItem();
            item.setCollectionId(collectionId);
            item.setCategoryCode(categoryCode);
            item.setFieldCode(fieldCode);
            item.setFieldName(config.getFieldName());
            item.setUnit(config.getUnit());

            // 数值/字符串处理
            BigDecimal numberValue = null;
            String stringValue = null;
            try {
                numberValue = new BigDecimal(value.toString());
            } catch (Exception e) {
                stringValue = value.toString();
            }
            item.setNumberValue(numberValue);
            item.setStringValue(stringValue);

            // 自动判断是否异常
            int isAbnormal = 0;
            int abnormalLevel = 0;
            if (numberValue != null) {
                BigDecimal min = null;
                BigDecimal max = null;

                // 男 → 取男参考范围
                if (gender == 2) {
                    min = config.getRefMinMale();
                    max = config.getRefMaxMale();
                }
                // 女 → 取女参考范围
                else if (gender == 1) {
                    min = config.getRefMinFemale();
                    max = config.getRefMaxFemale();
                }

                if (min != null && max != null) {
                    if (numberValue.compareTo(min) < 0 || numberValue.compareTo(max) > 0) {
                        isAbnormal = 1;
                        abnormalLevel = 1;
                    }
                }
            }
            item.setIsAbnormal(isAbnormal);
            item.setAbnormalLevel(abnormalLevel);
            item.setCreateTime(new Date());
            item.setUpdateTime(new Date());
            item.setCreateBy(username);
            itemList.add(item);
        }

        // 5：批量插入（原生 XML）
        if (!itemList.isEmpty()) {
            collectionDataItemMapper.insertBatch(itemList);
        }

        // 6：更新完成率
        updateCompleteness(collectionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchCategoryData(BatchCategorySaveDTO saveDTO) {
        Long collectionId = saveDTO.getCollectionId();
        List<CategorySaveDTO> categoryList = saveDTO.getCategoryList();
        if (CollectionUtils.isEmpty(categoryList)) {
            return;
        }

        // 1. 获取性别（只查一次）
        CollectionRecord record = collectionRecordMapper.selectById(collectionId);
        if (record == null) {
            throw new BusinessException("采集记录不存在");
        }
        Integer gender = record.getMemberGender();
        String username = SecurityUtils.getUsername();

        // 2. 先删除该采集记录的所有旧数据（避免重复插入）
        collectionDataItemMapper.deleteByCollectionId(collectionId);

        List<String> categoryCodeList = categoryList.stream()
                .map(CategorySaveDTO::getCategoryCode)
                .collect(Collectors.toList());
        // 3. 一次性加载所有字段配置（性能提升）
        List<CollectionFieldConfig> allConfigList = collectionFieldConfigMapper.selectByCategoryCodeList(categoryCodeList);
        Map<String, CollectionFieldConfig> configMap = allConfigList.stream()
                .collect(Collectors.toMap(
                        c -> c.getCategoryCode() + "_" + c.getFieldCode(),
                        c -> c
                ));

        // 4. 构建所有指标（所有分类）
        List<CollectionDataItem> finalItemList = new ArrayList<>();

        for (CategorySaveDTO dto : categoryList) {
            String categoryCode = dto.getCategoryCode();
            Map<String, Object> fieldData = dto.getFieldData();
            if (CollectionUtils.isEmpty(fieldData)) continue;

            for (Map.Entry<String, Object> entry : fieldData.entrySet()) {
                String fieldCode = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;

                // 从缓存拿配置
                CollectionFieldConfig config = configMap.get(categoryCode + "_" + fieldCode);
                if (config == null) continue;

                CollectionDataItem item = new CollectionDataItem();
                item.setCollectionId(collectionId);
                item.setCategoryCode(categoryCode);
                item.setFieldCode(fieldCode);
                item.setFieldName(config.getFieldName());
                item.setUnit(config.getUnit());

                // 数值处理
                BigDecimal numberValue = null;
                String stringValue = null;
                try {
                    numberValue = new BigDecimal(value.toString());
                } catch (Exception e) {
                    stringValue = value.toString();
                }
                item.setNumberValue(numberValue);
                item.setStringValue(stringValue);

                // 异常判断
                int isAbnormal = 0;
                int abnormalLevel = 0;
                if (numberValue != null) {
                    BigDecimal min = (gender == 2) ? config.getRefMinMale() : config.getRefMinFemale();
                    BigDecimal max = (gender == 2) ? config.getRefMaxMale() : config.getRefMaxFemale();

                    if (min != null && max != null) {
                        if (numberValue.compareTo(min) < 0 || numberValue.compareTo(max) > 0) {
                            isAbnormal = 1;
                            abnormalLevel = 1;
                        }
                    }
                }
                item.setIsAbnormal(isAbnormal);
                item.setAbnormalLevel(abnormalLevel);
                item.setCreateBy(username);
                item.setCreateTime(new Date());
                item.setUpdateTime(new Date());
                finalItemList.add(item);
            }
        }

        // 5. 批量插入（一次入库）
        if (!finalItemList.isEmpty()) {
            collectionDataItemMapper.insertBatch(finalItemList);
        }

        // 6. 更新完成率
        updateCompleteness(collectionId);
    }

    @Override
    public CollectionDetailVO getDetail(Long collectionId) {
        // 1. 查询采集主表（不变，只是字段名按最新表结构修正）
        CollectionRecord record = collectionRecordMapper.selectById(collectionId);
        if (record == null) {
            throw new BusinessException("采集记录不存在");
        }

        // 2. 组装主表VO（按最新表结构修正字段名）
        CollectionRecordVO recordVO = new CollectionRecordVO();
        recordVO.setId(record.getId());
        recordVO.setCollectionNo(record.getCollectionNo());
        recordVO.setMemberId(record.getMemberId());
        // 注意：主表已不存memberName/memberGender，这里从联查/会员表获取，或直接从item聚合时补
        // 若主表已冗余，保留；若已去冗余，需联查member_info，这里先按你前端返回结构补全
        recordVO.setMemberName(record.getMemberName());
        recordVO.setMemberGender(record.getMemberGender());
        recordVO.setHospital(record.getHospital());
        recordVO.setSourceType(record.getSourceType());
        recordVO.setReportStatus(record.getReportStatus());
        recordVO.setStatus(record.getFinishStatus());
        recordVO.setCreatedTime(record.getCreateTime());
        recordVO.setUpdatedTime(record.getUpdateTime());

        // 3. 从行式存储表查询所有指标数据（替代原JSON表查询）
        List<CollectionDataItem> itemList = collectionDataItemMapper.selectByCollectionId(collectionId);

        // 4. 按 category_code 分组，聚合为前端需要的结构
        Map<String, List<CollectionDataItem>> groupByCategory = itemList.stream()
                .collect(Collectors.groupingBy(CollectionDataItem::getCategoryCode));

        List<CollectionDetailVO.CategoryDataVO> categoryDataVOs = new ArrayList<>();

        for (Map.Entry<String, List<CollectionDataItem>> entry : groupByCategory.entrySet()) {
            String categoryCode = entry.getKey();
            List<CollectionDataItem> items = entry.getValue();

            CollectionDetailVO.CategoryDataVO cdVO = new CollectionDetailVO.CategoryDataVO();
            cdVO.setCategoryCode(categoryCode);

            // 5. 计算已填数量（行式表直接用size，无需冗余字段）
            cdVO.setFilledCount(items.size());
            // 6. 完成状态：可按必填项是否填完判断，这里先简化为false（和原逻辑一致）
            cdVO.setIsCompleted(false);

            // 7. 把多行指标，聚合为 {fieldCode: value} 的Map（完全还原前端需要的JSON结构）
            Map<String, Object> fieldData = new HashMap<>();
            for (CollectionDataItem item : items) {
                String fieldCode = item.getFieldCode();
                // 优先取数值，无数值则取字符串
                Object value = item.getNumberValue() != null ? item.getNumberValue() : item.getStringValue();
                fieldData.put(fieldCode, value);
            }
            cdVO.setFieldData(fieldData);

            categoryDataVOs.add(cdVO);
        }

        // 8. 组装最终返回VO（完全兼容原结构）
        CollectionDetailVO detailVO = new CollectionDetailVO();
        detailVO.setRecord(recordVO);
        detailVO.setCategoryDataList(categoryDataVOs);
        return detailVO;
    }

    @Override
    @Transactional
    public void complete(Long id) {
        CollectionRecord record = getById(id);
        record.setFinishStatus(1);
        record.setUpdateBy(SecurityUtils.getLoginUser().getUsername());
        record.setUpdateTime(DateUtils.getNowDate());
        collectionRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        getById(id);
        final int delete = collectionRecordMapper.deleteById(id);
        if (delete>0){
            collectionDataItemMapper.deleteByCollectionId(id);
        }
    }

    @Override
    @Transactional
    public void updateCompleteness(Long id) {
        CollectionRecord record = collectionRecordMapper.selectById(id);
        if (record == null) return;

        // 1. 行式存储：查询【已填写的指标总数】
        int totalFilled = collectionDataItemMapper.selectCountByCollectionId(id);

        // 2. 总指标数 = 配置表中所有启用的指标总数
        int totalFields = collectionFieldConfigMapper.selectCountAllEnabled();

        // 3. 计算百分比
        if (totalFields > 0) {
            BigDecimal pct = BigDecimal.valueOf(totalFilled)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalFields), 2, RoundingMode.HALF_UP);
            record.setCompleteness(pct);
        } else {
            record.setCompleteness(BigDecimal.ZERO);
        }

        // 更新
        collectionRecordMapper.updateById(record);
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getLoginUser().getUserId();
    }

    private boolean isAdmin() {
        return SecurityUtils.isAdmin(getCurrentUserId());
    }
}

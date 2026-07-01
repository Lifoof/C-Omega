package com.health.report.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.health.report.common.utils.DateUtils;
import com.health.report.common.utils.StringUtils;
import com.health.report.domain.CollectionCategoryConfig;
import com.health.report.mapper.CollectionCategoryConfigMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.health.report.mapper.CollectionFieldConfigMapper;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.service.ICollectionFieldConfigService;
import org.springframework.util.CollectionUtils;

/**
 * 指标管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-04-10
 */
@Service
public class CollectionFieldConfigServiceImpl implements ICollectionFieldConfigService
{
    @Autowired
    private CollectionFieldConfigMapper collectionFieldConfigMapper;

    @Autowired
    private CollectionCategoryConfigMapper collectionCategoryConfigMapper;



    /**
     * 查询指标管理
     *
     * @param id 指标管理主键
     * @return 指标管理
     */
    @Override
    public CollectionFieldConfig selectCollectionFieldConfigById(Long id)
    {
        return collectionFieldConfigMapper.selectCollectionFieldConfigById(id);
    }

    /**
     * 查询指标管理列表
     *
     * @param collectionFieldConfig 指标管理
     * @return 指标管理
     */
    @Override
    public List<CollectionFieldConfig> selectCollectionFieldConfigList(CollectionFieldConfig collectionFieldConfig)
    {
        return collectionFieldConfigMapper.selectCollectionFieldConfigList(collectionFieldConfig);
    }

    /**
     * 新增指标管理
     *
     * @param collectionFieldConfig 指标管理
     * @return 结果
     */
    @Override
    public int insertCollectionFieldConfig(CollectionFieldConfig collectionFieldConfig)
    {
        // 处理归属分类：不存在则新增，并回填 categoryCode
        handleCategory(collectionFieldConfig);
        collectionFieldConfig.setCreateTime(DateUtils.getNowDate());
        return collectionFieldConfigMapper.insertCollectionFieldConfig(collectionFieldConfig);
    }

    /**
     * 修改指标管理
     *
     * @param collectionFieldConfig 指标管理
     * @return 结果
     */
    @Override
    public int updateCollectionFieldConfig(CollectionFieldConfig collectionFieldConfig)
    {
        // 处理归属分类：不存在则新增，并回填 categoryCode
        handleCategory(collectionFieldConfig);
        collectionFieldConfig.setUpdateTime(DateUtils.getNowDate());
        return collectionFieldConfigMapper.updateCollectionFieldConfig(collectionFieldConfig);
    }

    /**
     * 判断字符串是否为空或等于"——"
     */
    private boolean isEmptyOrDash(String str) {
        return StringUtils.isBlank(str) || "——".equals(str);
    }

    /**
     * 处理归属分类：
     * 1. 根据 categoryName 查询分类
     * 2. 不存在则自动新增
     * 3. 把 categoryCode 设置回 fieldConfig
     */
    private void handleCategory(CollectionFieldConfig fieldConfig) {
        String categoryName = fieldConfig.getCategoryName();
        if (StringUtils.isBlank(categoryName)) {
            return;
        }
        // 3. 分类排序
        Integer maxSort = collectionCategoryConfigMapper.getMaxSortOrder();
        if (maxSort == null) maxSort = 0;
        // 1. 根据分类名称查询是否已存在
        CollectionCategoryConfig category = collectionCategoryConfigMapper.selectByCategoryName(categoryName);

        if (category == null) {
            // 2. 不存在 → 自动生成分类编码并新增
            category = new CollectionCategoryConfig();
            category.setCategoryCode(fieldConfig.getCategoryNameEn());
            category.setCategoryName(categoryName);
            category.setCategoryNameEn(fieldConfig.getCategoryNameEn());
            category.setGenderScope(0);
            category.setStatus(1);
            category.setDeleted(0);
            category.setCreateTime(new Date());
            category.setUpdateTime(new Date());
            maxSort++;
            category.setSortOrder(maxSort);

            collectionCategoryConfigMapper.insertCollectionCategoryConfig(category);
        }

        // 3. 把分类编码赋值给指标配置
        fieldConfig.setCategoryCode(category.getCategoryCode());
    }


    /**
     * 批量删除指标管理
     *
     * @param ids 需要删除的指标管理主键
     * @return 结果
     */
    @Override
    public int deleteCollectionFieldConfigByIds(Long[] ids)
    {
        // 检查是否有指标被模型参数使用
        Long usedId = collectionFieldConfigMapper.checkUsedByModelParamBatch(ids);
        if (usedId != null) {
            // 查询被使用的指标名称
            CollectionFieldConfig usedConfig = collectionFieldConfigMapper.selectCollectionFieldConfigById(usedId);
            String fieldName = usedConfig != null ? usedConfig.getFieldName() : "未知指标";
            throw new RuntimeException("指标 [" + fieldName + "] 正在被模型参数使用，无法删除！");
        }
        return collectionFieldConfigMapper.deleteCollectionFieldConfigByIds(ids);
    }

    /**
     * 删除指标管理信息
     *
     * @param id 指标管理主键
     * @return 结果
     */
    @Override
    public int deleteCollectionFieldConfigById(Long id)
    {
        // 检查是否被模型参数使用
        int count = collectionFieldConfigMapper.checkUsedByModelParam(id);
        if (count > 0) {
            CollectionFieldConfig config = collectionFieldConfigMapper.selectCollectionFieldConfigById(id);
            String fieldName = config != null ? config.getFieldName() : "未知指标";
            throw new RuntimeException("指标 [" + fieldName + "] 正在被模型参数使用，无法删除！");
        }
        return collectionFieldConfigMapper.deleteCollectionFieldConfigById(id);
    }

    @Override
    public String importCollectionFieldConfig(List<CollectionFieldConfig> configList, String lang) {
        if (CollectionUtils.isEmpty(configList)) {
            return "en".equals(lang) ? "Import data cannot be empty!" : "导入数据不能为空！";
        }

        int successNum = 0;
        int failNum = 0;
        StringBuilder errMsg = new StringBuilder();
        List<CollectionFieldConfig> insertList = new ArrayList<>();

        for (CollectionFieldConfig config : configList) {
            try {
                // 基础字段
                String fieldName = config.getFieldName();
                String categoryName = config.getCategoryName();
                String fieldNameEn = config.getFieldNameEn();
                String categoryNameEn = config.getCategoryNameEn();
                String fieldParamName = config.getFieldParamName();

                // ==============================================
                // 🔥 核心判断：是否是 衍生变量（英文分类固定值）
                // ==============================================
                boolean isDerivative = "Derivative variables".equals(categoryNameEn);

                // ==============================================
                // 1. 非衍生变量 → 必须校验指标名称不能为空
                // ==============================================
                if (!isDerivative) {
                    if (StringUtils.isBlank(fieldName) || StringUtils.isBlank(categoryName)) {
                        failNum++;
                        String msg = "en".equals(lang)
                                ? "Row " + (successNum + failNum) + ": Indicator name/category cannot be empty!<br/>"
                                : "行" + (successNum + failNum) + "：指标名称/归属类别不能为空！<br/>";
                        errMsg.append(msg);
                        continue;
                    }
                    if (StringUtils.isBlank(fieldNameEn) || StringUtils.isBlank(categoryNameEn)) {
                        failNum++;
                        String msg = "en".equals(lang)
                                ? "Row " + (successNum + failNum) + ": English indicator name/English category cannot be empty!<br/>"
                                : "行" + (successNum + failNum) + "：指标名称英文/归属类别英文不能为空！<br/>";
                        errMsg.append(msg);
                        continue;
                    }
                }
                // 衍生变量：不校验指标名称为空！！！

                // ==============================================
                // 2. 🔥 只有衍生变量：把 fieldParamName → fieldName
                // ==============================================
                if (isDerivative) {
                    // 用模型参数名称覆盖指标名称（你要的逻辑）
                    config.setFieldName(fieldParamName);
                    // 英文指标名也可以同步（可选）
                    config.setFieldNameEn(fieldNameEn);
                }

                // ========== 自动处理归属类别（不变） ==========
                handleCategory(config);

                // ========== 根据 指标名称 + categoryCode 判断是否存在 ==========
                CollectionFieldConfig existConfig =
                        collectionFieldConfigMapper.selectByFieldNameAndCategoryCode(
                                config.getFieldName(),
                                config.getCategoryCode()
                        );

                if (existConfig != null) {
                    // 更新
                    existConfig.setAliasName(config.getAliasName());
                    existConfig.setAliasNameEn(config.getAliasNameEn());
                    existConfig.setUnit(!"——".equals(config.getUnit()) ? config.getUnit() : null);
                    existConfig.setUnitEn(!"——".equals(config.getUnitEn()) ? config.getUnitEn() : null);
                    existConfig.setRefRangeText(!"——".equals(config.getRefRangeText()) ? config.getRefRangeText() : null);
                    existConfig.setRefRangeTextEn(!"——".equals(config.getRefRangeTextEn()) ? config.getRefRangeTextEn() : null);
                    existConfig.setRemark(config.getRemark());
                    existConfig.setRemarkEn(config.getRemarkEn());
                    existConfig.setFieldParamName(config.getFieldParamName());
                    existConfig.setCategoryNameEn(config.getCategoryNameEn());
                    existConfig.setFieldNameEn(config.getFieldNameEn());
                    existConfig.setFieldType("NUMBER");
                    existConfig.setUpdateTime(new Date());
                    // 衍生变量更新时也同步名称
                    if (isDerivative) {
                        existConfig.setFieldName(config.getFieldName());
                    }

                    collectionFieldConfigMapper.updateCollectionFieldConfig(existConfig);
                    successNum++;
                } else {
                    // 新增
                    config.setCreateTime(DateUtils.getNowDate());
                    config.setFieldCode(config.getFieldNameEn());
                    config.setUnit(!"——".equals(config.getUnit()) ? config.getUnit() : null);
                    config.setUnitEn(!"——".equals(config.getUnitEn()) ? config.getUnitEn() : null);
                    config.setRefRangeText(!"——".equals(config.getRefRangeText()) ? config.getRefRangeText() : null);
                    config.setRefRangeTextEn(!"——".equals(config.getRefRangeTextEn()) ? config.getRefRangeTextEn() : null);
                    // 判断 fieldType：如果单位、单位英文、参考值/范围、参考值/范围英文都为空，则为 Text
                    /*boolean isTextType = isEmptyOrDash(config.getUnit()) &&
                            isEmptyOrDash(config.getUnitEn()) &&
                            StringUtils.isBlank(config.getRefRangeText()) &&
                            StringUtils.isBlank(config.getRefRangeTextEn());
                    config.setFieldType(isTextType ? "TEXT" : "NUMBER");*/
                    config.setFieldType("NUMBER");
                    config.setDeleted(0);
                    config.setStatus(1);
                    config.setGenderScope(0);
                    config.setCreateTime(new Date());
                    insertList.add(config);
                }

            } catch (Exception e) {
                failNum++;
                String msg = "en".equals(lang)
                        ? "Row " + (successNum + failNum) + ": Import failed! " + e.getMessage() + "<br/>"
                        : "行" + (successNum + failNum) + "：导入失败！" + e.getMessage() + "<br/>";
                errMsg.append(msg);
            }
        }

        // 排序逻辑（完全不变）
        if (ObjectUtils.isNotEmpty(insertList)) {
            Map<String, List<CollectionFieldConfig>> groupByCategory = insertList.stream()
                    .collect(Collectors.groupingBy(CollectionFieldConfig::getCategoryCode));

            for (Map.Entry<String, List<CollectionFieldConfig>> entry : groupByCategory.entrySet()) {
                String categoryCode = entry.getKey();
                List<CollectionFieldConfig> fields = entry.getValue();

                Integer maxSort = collectionFieldConfigMapper.getMaxSortOrderByCategory(categoryCode);
                if (maxSort == null) maxSort = 0;

                for (CollectionFieldConfig field : fields) {
                    maxSort++;
                    field.setSortOrder(maxSort);
                }
            }
            collectionFieldConfigMapper.batchInsertFields(insertList);
            successNum += insertList.size();
        }

        // 顶部导入完成统计（中英文）
        String totalMsg = "en".equals(lang)
                ? "Import completed! Success: " + successNum + " items, Failed: " + failNum + " items<br/>"
                : "导入完成！成功：" + successNum + " 条，失败：" + failNum + " 条<br/>";
        errMsg.insert(0, totalMsg);
        return errMsg.toString();
    }
}

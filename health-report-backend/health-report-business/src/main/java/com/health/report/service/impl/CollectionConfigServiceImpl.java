package com.health.report.service.impl;

import com.health.report.domain.CollectionCategoryConfig;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.dto.collection.CategoryConfigVO;
import com.health.report.dto.collection.FieldConfigVO;
import com.health.report.mapper.CollectionCategoryConfigMapper;
import com.health.report.mapper.CollectionFieldConfigMapper;
import com.health.report.service.ICollectionConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采集配置Service业务层处理
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@Service
public class CollectionConfigServiceImpl implements ICollectionConfigService {

    @Autowired
    private CollectionCategoryConfigMapper categoryMapper;

    @Autowired
    private CollectionFieldConfigMapper fieldMapper;


    public List<CategoryConfigVO> listCategoriesWithFields(Integer gender, String categoryName) {
        CollectionCategoryConfig categoryConfig = new CollectionCategoryConfig();
        categoryConfig.setStatus(1);
        categoryConfig.setCategoryName(categoryName);
        List<CollectionCategoryConfig> categories = categoryMapper.selectCollectionCategoryConfigListNoDerivedVariables(categoryConfig);

        CollectionFieldConfig fieldConfig = new CollectionFieldConfig();
        fieldConfig.setStatus(1);
        List<CollectionFieldConfig> allFields = fieldMapper.selectCollectionFieldConfigList(fieldConfig);

        Map<String, List<CollectionFieldConfig>> fieldsByCategory = allFields.stream()
                .collect(Collectors.groupingBy(CollectionFieldConfig::getCategoryCode));

        List<CategoryConfigVO> result = new ArrayList<>();
        for (CollectionCategoryConfig cat : categories) {
            if (gender != null && cat.getGenderScope() != 0 && !cat.getGenderScope().equals(gender)) {
                continue;
            }

            CategoryConfigVO vo = new CategoryConfigVO();
            vo.setId(cat.getId());
            vo.setCategoryCode(cat.getCategoryCode());
            vo.setCategoryName(cat.getCategoryName());
            vo.setCategoryNameEn(cat.getCategoryNameEn());
            vo.setGenderScope(cat.getGenderScope());
            vo.setIcon(cat.getIcon());
            vo.setSortOrder(cat.getSortOrder());

            List<CollectionFieldConfig> catFields = fieldsByCategory.getOrDefault(cat.getCategoryCode(), List.of());
            List<FieldConfigVO> fieldVOs = new ArrayList<>();
            for (CollectionFieldConfig f : catFields) {
                if (gender != null && f.getGenderScope() != 0 && !f.getGenderScope().equals(gender)) {
                    continue;
                }
                FieldConfigVO fv = new FieldConfigVO();
                fv.setId(f.getId());
                fv.setFieldCode(f.getFieldCode());
                fv.setFieldName(f.getFieldName());
                fv.setFieldNameEn(f.getFieldNameEn());
                fv.setAliasName(f.getAliasName());
                fv.setAliasNameEn(f.getAliasNameEn());
                fv.setRemark(f.getRemark());
                fv.setRemarkEn(f.getRemarkEn());
                fv.setCategoryCode(f.getCategoryCode());
                fv.setGenderScope(f.getGenderScope());
                fv.setFieldType(f.getFieldType());
                fv.setUnit(f.getUnit());
                fv.setUnitEn(f.getUnitEn());
                fv.setRefRangeText(f.getRefRangeText());
                fv.setRefRangeTextEn(f.getRefRangeTextEn());
                fv.setDescription(f.getDescription());
                fv.setIsRequired(f.getIsRequired());
                fv.setEnumOptions(f.getEnumOptions());
                fv.setSortOrder(f.getSortOrder());
                fieldVOs.add(fv);
            }
            vo.setFields(fieldVOs);
            result.add(vo);
        }
        return result;
    }

    public List<CategoryConfigVO> listModelCategoriesWithFields(Integer gender, String categoryName) {
        CollectionCategoryConfig categoryConfig = new CollectionCategoryConfig();
        categoryConfig.setStatus(1);
        categoryConfig.setCategoryName(categoryName);
        List<CollectionCategoryConfig> categories = categoryMapper.selectCollectionCategoryConfigList(categoryConfig);

        CollectionFieldConfig fieldConfig = new CollectionFieldConfig();
        fieldConfig.setStatus(1);
        List<CollectionFieldConfig> allFields = fieldMapper.selectCollectionFieldConfigList(fieldConfig);

        Map<String, List<CollectionFieldConfig>> fieldsByCategory = allFields.stream()
                .collect(Collectors.groupingBy(CollectionFieldConfig::getCategoryCode));

        List<CategoryConfigVO> result = new ArrayList<>();
        for (CollectionCategoryConfig cat : categories) {
            if (gender != null && cat.getGenderScope() != 0 && !cat.getGenderScope().equals(gender)) {
                continue;
            }

            CategoryConfigVO vo = new CategoryConfigVO();
            vo.setId(cat.getId());
            vo.setCategoryCode(cat.getCategoryCode());
            vo.setCategoryName(cat.getCategoryName());
            vo.setCategoryNameEn(cat.getCategoryNameEn());
            vo.setGenderScope(cat.getGenderScope());
            vo.setIcon(cat.getIcon());
            vo.setSortOrder(cat.getSortOrder());

            List<CollectionFieldConfig> catFields = fieldsByCategory.getOrDefault(cat.getCategoryCode(), List.of());
            List<FieldConfigVO> fieldVOs = new ArrayList<>();
            for (CollectionFieldConfig f : catFields) {
                if (gender != null && f.getGenderScope() != 0 && !f.getGenderScope().equals(gender)) {
                    continue;
                }
                FieldConfigVO fv = new FieldConfigVO();
                fv.setId(f.getId());
                fv.setFieldCode(f.getFieldCode());
                fv.setFieldName(f.getFieldName());
                fv.setFieldNameEn(f.getFieldNameEn());
                fv.setAliasName(f.getAliasName());
                fv.setAliasNameEn(f.getAliasNameEn());
                fv.setRemark(f.getRemark());
                fv.setRemarkEn(f.getRemarkEn());
                fv.setCategoryCode(f.getCategoryCode());
                fv.setGenderScope(f.getGenderScope());
                fv.setFieldType(f.getFieldType());
                fv.setUnit(f.getUnit());
                fv.setUnitEn(f.getUnitEn());
                fv.setRefRangeText(f.getRefRangeText());
                fv.setRefRangeTextEn(f.getRefRangeTextEn());
                fv.setDescription(f.getDescription());
                fv.setIsRequired(f.getIsRequired());
                fv.setEnumOptions(f.getEnumOptions());
                fv.setSortOrder(f.getSortOrder());
                fieldVOs.add(fv);
            }
            vo.setFields(fieldVOs);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<String> getCollectionConfigName() {
        return fieldMapper.getCollectionConfigName();
    }

    @Override
    public List<String> getCollectionConfigNameEn() {
        return fieldMapper.getCollectionConfigNameEn();
    }

    @Override
    public List<CollectionFieldConfig> getCollectionFieldConfigList() {
        CollectionFieldConfig fieldConfig = new CollectionFieldConfig();
        fieldConfig.setStatus(1);
        return fieldMapper.selectCollectionFieldConfigList(fieldConfig);
    }

    public int countFieldsByGender(Integer gender) {
        CollectionFieldConfig fieldConfig = new CollectionFieldConfig();
        fieldConfig.setStatus(1);
        List<CollectionFieldConfig> all = fieldMapper.selectCollectionFieldConfigList(fieldConfig);
        return (int) all.stream()
                .filter(f -> gender == null || f.getGenderScope() == 0 || f.getGenderScope().equals(gender))
                .count();
    }

    public int countRequiredByGender(Integer gender) {
        CollectionFieldConfig fieldConfig = new CollectionFieldConfig();
        fieldConfig.setStatus(1);
        fieldConfig.setIsRequired(1);
        List<CollectionFieldConfig> all = fieldMapper.selectCollectionFieldConfigList(fieldConfig);
        return (int) all.stream()
                .filter(f -> gender == null || f.getGenderScope() == 0 || f.getGenderScope().equals(gender))
                .count();
    }
}

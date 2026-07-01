package com.health.report.service;

import com.health.report.domain.CollectionFieldConfig;
import com.health.report.dto.collection.CategoryConfigVO;

import java.util.List;

/**
 * 采集配置Service接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface ICollectionConfigService {
    List<CategoryConfigVO> listCategoriesWithFields(Integer gender,String categoryName);

    List<CategoryConfigVO> listModelCategoriesWithFields(Integer gender,String categoryName);

    List<String> getCollectionConfigName();

    List<String> getCollectionConfigNameEn();

    /**
     * 获取指标字段配置列表（包含别名等信息）
     * @return 字段配置列表
     */
    List<CollectionFieldConfig> getCollectionFieldConfigList();
}

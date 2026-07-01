package com.health.report.mapper;


import com.health.report.domain.AiModel;
import com.health.report.domain.CollectionCategoryConfig;
import com.health.report.domain.CollectionFieldConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface CollectionCategoryConfigMapper {
    List<CollectionCategoryConfig> selectCollectionCategoryConfigList(CollectionCategoryConfig collectionCategoryConfig);

    List<CollectionCategoryConfig> selectCollectionCategoryConfigListNoDerivedVariables(CollectionCategoryConfig collectionCategoryConfig);
    List<CollectionCategoryConfig> listByCategoryNames(@Param("categoryNames") Set<String> categoryNames);
    void batchInsertCategories(@Param("list") List<CollectionCategoryConfig> list);

    public int insertCollectionCategoryConfig(CollectionCategoryConfig collectionCategoryConfig);

    // 获取最大排序号
    Integer getMaxSortOrder();
    /**
     * 根据分类名称查询分类
     */
    CollectionCategoryConfig selectByCategoryName(String categoryName);

    /**
     * 根据分类编码列表查询分类
     */
    List<CollectionCategoryConfig> listByCategoryCodes(@Param("categoryCodes") Set<String> categoryCodes);



}

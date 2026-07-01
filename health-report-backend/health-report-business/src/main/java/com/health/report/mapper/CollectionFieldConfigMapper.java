package com.health.report.mapper;


import com.health.report.domain.CollectionCategoryConfig;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.dto.model.FieldParamImport;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CollectionFieldConfigMapper {

    /**
     * 查询指标管理
     *
     * @param id 指标管理主键
     * @return 指标管理
     */
    public CollectionFieldConfig selectCollectionFieldConfigById(Long id);

    /**
     * 新增指标管理
     *
     * @param collectionFieldConfig 指标管理
     * @return 结果
     */
    public int insertCollectionFieldConfig(CollectionFieldConfig collectionFieldConfig);

    /**
     * 修改指标管理
     *
     * @param collectionFieldConfig 指标管理
     * @return 结果
     */
    public int updateCollectionFieldConfig(CollectionFieldConfig collectionFieldConfig);

    /**
     * 删除指标管理
     *
     * @param id 指标管理主键
     * @return 结果
     */
    public int deleteCollectionFieldConfigById(Long id);

    /**
     * 批量删除指标管理
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCollectionFieldConfigByIds(Long[] ids);
    List<CollectionFieldConfig> selectCollectionFieldConfigList(CollectionFieldConfig collectionFieldConfig);

    List<CollectionFieldConfig> selectByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * 查询系统 总指标数（所有启用的）
     */
    Integer selectCountAllEnabled();

    List<String> getCollectionConfigName();

    List<String> getCollectionConfigNameEn();

    /**
     * 根据字段名称查询字段配置
     */
    CollectionFieldConfig selectByFieldName(@Param("fieldName") String fieldName);

    /**
     * 根据多个分类编码批量查询
     */
    List<CollectionFieldConfig> selectByCategoryCodeList(@Param("categoryCodeList") List<String> categoryCodeList);

    List<CollectionFieldConfig> listByCategoryAndFieldNames(
            @Param("list") List<FieldParamImport> importList,
            @Param("categoryMap") Map<String, CollectionCategoryConfig> categoryMap
    );

    void batchInsertFields(@Param("list") List<CollectionFieldConfig> list);

    // 根据分类编码获取该分类下最大的参数排序号
    Integer getMaxSortOrderByCategory(@Param("categoryCode") String categoryCode);

    List<Long> selectIdsByFieldCodes(@Param("list") List<String> fieldCodes);

    List<CollectionFieldConfig> selectParamsByModelId(@Param("modelId") Long modelId);

    /**
     * 根据 指标名称 + 分类编码 查询是否存在
     */
    CollectionFieldConfig selectByFieldNameAndCategoryCode(
            @Param("fieldName") String fieldName,
            @Param("categoryCode") String categoryCode
    );

    /**
     * 根据指标名称列表查询已存在的参数
     */
    List<CollectionFieldConfig> listByFieldNames(@Param("fieldNames") Set<String> fieldNames);

    /**
     * 检查指标是否被模型参数使用
     * @param id 指标ID
     * @return 使用数量
     */
    Integer checkUsedByModelParam(@Param("id") Long id);

    /**
     * 批量检查指标是否被模型参数使用
     * @param ids 指标ID数组
     * @return 被使用的指标ID（如果有）
     */
    Long checkUsedByModelParamBatch(@Param("array") Long[] ids);

}

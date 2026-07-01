package com.health.report.service;

import java.util.List;
import com.health.report.domain.CollectionCategoryConfig;

/**
 * 采集分组配置Service接口
 * 
 * @author ruoyi
 * @date 2026-03-27
 */
public interface ICollectionCategoryConfigService 
{
    /**
     * 查询采集分组配置
     * 
     * @param id 采集分组配置主键
     * @return 采集分组配置
     */
    public CollectionCategoryConfig selectCollectionCategoryConfigById(Long id);

    /**
     * 查询采集分组配置列表
     * 
     * @param collectionCategoryConfig 采集分组配置
     * @return 采集分组配置集合
     */
    public List<CollectionCategoryConfig> selectCollectionCategoryConfigList(CollectionCategoryConfig collectionCategoryConfig);

    /**
     * 新增采集分组配置
     * 
     * @param collectionCategoryConfig 采集分组配置
     * @return 结果
     */
    public int insertCollectionCategoryConfig(CollectionCategoryConfig collectionCategoryConfig);

    /**
     * 修改采集分组配置
     * 
     * @param collectionCategoryConfig 采集分组配置
     * @return 结果
     */
    public int updateCollectionCategoryConfig(CollectionCategoryConfig collectionCategoryConfig);

    /**
     * 批量删除采集分组配置
     * 
     * @param ids 需要删除的采集分组配置主键集合
     * @return 结果
     */
    public int deleteCollectionCategoryConfigByIds(Long[] ids);

    /**
     * 删除采集分组配置信息
     * 
     * @param id 采集分组配置主键
     * @return 结果
     */
    public int deleteCollectionCategoryConfigById(Long id);
}

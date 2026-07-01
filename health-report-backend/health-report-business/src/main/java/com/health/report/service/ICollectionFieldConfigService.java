package com.health.report.service;

import java.util.List;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.domain.MemberInfo;

/**
 * 采集字段配置Service接口
 * 
 * @author ruoyi
 * @date 2026-03-27
 */
public interface ICollectionFieldConfigService 
{
    /**
     * 查询采集字段配置
     * 
     * @param id 采集字段配置主键
     * @return 采集字段配置
     */
    public CollectionFieldConfig selectCollectionFieldConfigById(Long id);

    /**
     * 查询采集字段配置列表
     * 
     * @param collectionFieldConfig 采集字段配置
     * @return 采集字段配置集合
     */
    public List<CollectionFieldConfig> selectCollectionFieldConfigList(CollectionFieldConfig collectionFieldConfig);

    /**
     * 新增采集字段配置
     * 
     * @param collectionFieldConfig 采集字段配置
     * @return 结果
     */
    public int insertCollectionFieldConfig(CollectionFieldConfig collectionFieldConfig);

    /**
     * 修改采集字段配置
     * 
     * @param collectionFieldConfig 采集字段配置
     * @return 结果
     */
    public int updateCollectionFieldConfig(CollectionFieldConfig collectionFieldConfig);

    /**
     * 批量删除采集字段配置
     * 
     * @param ids 需要删除的采集字段配置主键集合
     * @return 结果
     */
    public int deleteCollectionFieldConfigByIds(Long[] ids);

    /**
     * 删除采集字段配置信息
     * 
     * @param id 采集字段配置主键
     * @return 结果
     */
    public int deleteCollectionFieldConfigById(Long id);

    /**
     * 导入指标数据
     *
     * @param configList 指标数据列表
     * @return 结果
     */
    public String importCollectionFieldConfig(List<CollectionFieldConfig> configList,String lang);
}

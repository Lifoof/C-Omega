package com.health.report.service;

import java.util.List;
import com.health.report.domain.AgeThreshold;

/**
 * 指标年龄阈值Service接口
 * 
 * @author ruoyi
 * @date 2026-04-21
 */
public interface IAgeThresholdService 
{
    /**
     * 查询指标年龄阈值
     * 
     * @param id 指标年龄阈值主键
     * @return 指标年龄阈值
     */
    public AgeThreshold selectAgeThresholdById(Long id);

    /**
     * 查询指标年龄阈值列表
     * 
     * @param ageThreshold 指标年龄阈值
     * @return 指标年龄阈值集合
     */
    public List<AgeThreshold> selectAgeThresholdList(AgeThreshold ageThreshold);

    /**
     * 新增指标年龄阈值
     * 
     * @param ageThreshold 指标年龄阈值
     * @return 结果
     */
    public int insertAgeThreshold(AgeThreshold ageThreshold);

    /**
     * 修改指标年龄阈值
     * 
     * @param ageThreshold 指标年龄阈值
     * @return 结果
     */
    public int updateAgeThreshold(AgeThreshold ageThreshold);

    /**
     * 批量删除指标年龄阈值
     * 
     * @param ids 需要删除的指标年龄阈值主键集合
     * @return 结果
     */
    public int deleteAgeThresholdByIds(Long[] ids);

    /**
     * 删除指标年龄阈值信息
     * 
     * @param id 指标年龄阈值主键
     * @return 结果
     */
    public int deleteAgeThresholdById(Long id);

    /**
     * 批量插入 + 重复更新
     */
    void batchInsertWithUpdate(List<AgeThreshold> list);
}

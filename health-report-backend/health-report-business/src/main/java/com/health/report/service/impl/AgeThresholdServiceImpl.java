package com.health.report.service.impl;

import java.util.List;
import com.health.report.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.health.report.mapper.AgeThresholdMapper;
import com.health.report.domain.AgeThreshold;
import com.health.report.service.IAgeThresholdService;

/**
 * 指标年龄阈值Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-21
 */
@Service
public class AgeThresholdServiceImpl implements IAgeThresholdService 
{
    @Autowired
    private AgeThresholdMapper ageThresholdMapper;

    /**
     * 查询指标年龄阈值
     * 
     * @param id 指标年龄阈值主键
     * @return 指标年龄阈值
     */
    @Override
    public AgeThreshold selectAgeThresholdById(Long id)
    {
        return ageThresholdMapper.selectAgeThresholdById(id);
    }

    /**
     * 查询指标年龄阈值列表
     * 
     * @param ageThreshold 指标年龄阈值
     * @return 指标年龄阈值
     */
    @Override
    public List<AgeThreshold> selectAgeThresholdList(AgeThreshold ageThreshold)
    {
        return ageThresholdMapper.selectAgeThresholdList(ageThreshold);
    }

    /**
     * 新增指标年龄阈值
     * 
     * @param ageThreshold 指标年龄阈值
     * @return 结果
     */
    @Override
    public int insertAgeThreshold(AgeThreshold ageThreshold)
    {
        ageThreshold.setCreateTime(DateUtils.getNowDate());
        return ageThresholdMapper.insertAgeThreshold(ageThreshold);
    }

    /**
     * 修改指标年龄阈值
     * 
     * @param ageThreshold 指标年龄阈值
     * @return 结果
     */
    @Override
    public int updateAgeThreshold(AgeThreshold ageThreshold)
    {
        ageThreshold.setUpdateTime(DateUtils.getNowDate());
        return ageThresholdMapper.updateAgeThreshold(ageThreshold);
    }

    /**
     * 批量删除指标年龄阈值
     * 
     * @param ids 需要删除的指标年龄阈值主键
     * @return 结果
     */
    @Override
    public int deleteAgeThresholdByIds(Long[] ids)
    {
        return ageThresholdMapper.deleteAgeThresholdByIds(ids);
    }

    /**
     * 删除指标年龄阈值信息
     * 
     * @param id 指标年龄阈值主键
     * @return 结果
     */
    @Override
    public int deleteAgeThresholdById(Long id)
    {
        return ageThresholdMapper.deleteAgeThresholdById(id);
    }

    @Override
    public void batchInsertWithUpdate(List<AgeThreshold> list) {
        ageThresholdMapper.batchInsertWithUpdate( list);
    }
}

package com.health.report.service.impl;

import java.util.List;
import com.health.report.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.health.report.mapper.JsonRecordMapper;
import com.health.report.domain.JsonRecord;
import com.health.report.service.IJsonRecordService;

/**
 * JSON记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-09
 */
@Service
public class JsonRecordServiceImpl implements IJsonRecordService 
{
    @Autowired
    private JsonRecordMapper jsonRecordMapper;

    /**
     * 查询JSON记录
     * 
     * @param id JSON记录主键
     * @return JSON记录
     */
    @Override
    public JsonRecord selectJsonRecordById(Long id)
    {
        return jsonRecordMapper.selectJsonRecordById(id);
    }

    /**
     * 查询JSON记录列表
     * 
     * @param jsonRecord JSON记录
     * @return JSON记录
     */
    @Override
    public List<JsonRecord> selectJsonRecordList(JsonRecord jsonRecord)
    {
        return jsonRecordMapper.selectJsonRecordList(jsonRecord);
    }

    /**
     * 新增JSON记录
     * 
     * @param jsonRecord JSON记录
     * @return 结果
     */
    @Override
    public int insertJsonRecord(JsonRecord jsonRecord)
    {
        jsonRecord.setCreateTime(DateUtils.getNowDate());
        return jsonRecordMapper.insertJsonRecord(jsonRecord);
    }

    /**
     * 修改JSON记录
     * 
     * @param jsonRecord JSON记录
     * @return 结果
     */
    @Override
    public int updateJsonRecord(JsonRecord jsonRecord)
    {
        jsonRecord.setUpdateTime(DateUtils.getNowDate());
        return jsonRecordMapper.updateJsonRecord(jsonRecord);
    }

    /**
     * 批量删除JSON记录
     * 
     * @param ids 需要删除的JSON记录主键
     * @return 结果
     */
    @Override
    public int deleteJsonRecordByIds(Long[] ids)
    {
        return jsonRecordMapper.deleteJsonRecordByIds(ids);
    }

    /**
     * 删除JSON记录信息
     * 
     * @param id JSON记录主键
     * @return 结果
     */
    @Override
    public int deleteJsonRecordById(Long id)
    {
        return jsonRecordMapper.deleteJsonRecordById(id);
    }
}

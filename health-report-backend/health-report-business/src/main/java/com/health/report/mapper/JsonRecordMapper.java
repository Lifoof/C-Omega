package com.health.report.mapper;

import java.util.List;
import com.health.report.domain.JsonRecord;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;
import org.springframework.jdbc.core.SqlProvider;

/**
 * JSON记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-09
 */
public interface JsonRecordMapper 
{
    /**
     * 查询JSON记录
     * 
     * @param id JSON记录主键
     * @return JSON记录
     */
    public JsonRecord selectJsonRecordById(Long id);

    /**
     * 查询JSON记录列表
     * 
     * @param jsonRecord JSON记录
     * @return JSON记录集合
     */
    public List<JsonRecord> selectJsonRecordList(JsonRecord jsonRecord);

    /**
     * 新增JSON记录
     * 
     * @param jsonRecord JSON记录
     * @return 结果
     */
    public int insertJsonRecord(JsonRecord jsonRecord);

    /**
     * 修改JSON记录
     * 
     * @param jsonRecord JSON记录
     * @return 结果
     */
    public int updateJsonRecord(JsonRecord jsonRecord);

    /**
     * 删除JSON记录
     * 
     * @param id JSON记录主键
     * @return 结果
     */
    public int deleteJsonRecordById(Long id);

    /**
     * 批量删除JSON记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteJsonRecordByIds(Long[] ids);

    // 批量插入
    int insertBatch(@Param("list") List<JsonRecord> list);

    List<JsonRecord> selectByReportId(@Param("reportId") Long reportId);
}

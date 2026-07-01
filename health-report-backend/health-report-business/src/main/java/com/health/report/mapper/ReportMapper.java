package com.health.report.mapper;


import com.health.report.domain.Report;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ReportMapper {
    /**
     * 查询健康报告
     *
     * @param id 健康报告主键
     * @return 健康报告
     */
    public Report selectReportById(Long id);

    /**
     * 查询健康报告列表
     *
     * @param report 健康报告
     * @return 健康报告集合
     */
    public List<Report> selectReportList(Report report);

    /**
     * 新增健康报告
     *
     * @param report 健康报告
     * @return 结果
     */
    public int insertReport(Report report);

    /**
     * 修改健康报告
     *
     * @param report 健康报告
     * @return 结果
     */
    public int updateReport(Report report);

    /**
     * 删除健康报告
     *
     * @param id 健康报告主键
     * @return 结果
     */
    public int deleteReportById(Long id);

    /**
     * 批量删除健康报告
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteReportByIds(Long[] ids);

    /**
     * 查询健康报告数量
     *
     * @param report 健康报告查询条件
     * @return 报告数量
     */
    public long selectCount(Report report);

    /**
     * 查询使用最多的模型
     * @param userId 不传=查全部，传=查该用户的
     * @return { modelName, useCount }
     */
    Map<String, Object> selectMostUsedModel(@Param("userId") Long userId, @Param("start") Date start);

    List<Map<String, Object>> selectModelUseStats(@Param("userId") Long userId, @Param("start") LocalDateTime start);
}

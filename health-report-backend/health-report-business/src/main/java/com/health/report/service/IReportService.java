package com.health.report.service;

import java.util.List;
import com.health.report.domain.Report;

/**
 * 健康报告Service接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface IReportService
{
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
     * 批量删除健康报告
     *
     * @param ids 需要删除的健康报告主键集合
     * @return 结果
     */
    public int deleteReportByIds(Long[] ids);

    /**
     * 删除健康报告信息
     *
     * @param id 健康报告主键
     * @return 结果
     */
    public int deleteReportById(Long id);

    public List<Report> generateReportBatch(Long collectionId, List<Long> modelIds);
}

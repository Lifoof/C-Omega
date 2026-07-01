package com.health.report.mapper;

import com.health.report.domain.ReportModelResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;
/**
 * 报告模型结果Mapper接口
 */
public interface ReportModelResultMapper {

    /**
     * 插入模型结果
     */
    int insert(ReportModelResult result);

    /**
     * 批量插入模型结果
     */
    int batchInsert(@Param("list") List<ReportModelResult> list);

    /**
     * 根据报告ID查询模型结果列表
     */
    List<ReportModelResult> selectByReportId(@Param("reportId") Long reportId);

    /**
     * 根据报告ID和分类查询模型结果
     */
    ReportModelResult selectByReportIdAndCategory(@Param("reportId") Long reportId,
                                                  @Param("category") String category);

    /**
     * 根据报告ID删除模型结果
     */
    int deleteByReportId(@Param("reportId") Long reportId);

    /**
     * 根据ID删除模型结果
     */
    int deleteById(@Param("id") Long id);

    /**
     * 查询报告的所有模型结果（按优先级排序）
     */
    List<ReportModelResult> selectByReportIdOrderByPriority(@Param("reportId") Long reportId);

    /**
     * 查询优先级最高的模型结果（priority值最小的）
     */
    ReportModelResult selectTopPriorityByReportId(@Param("reportId") Long reportId);
}

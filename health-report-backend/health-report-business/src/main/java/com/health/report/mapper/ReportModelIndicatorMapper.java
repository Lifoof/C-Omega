package com.health.report.mapper;

import com.health.report.domain.ReportModelIndicator;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模型结果指标明细Mapper
 */
public interface ReportModelIndicatorMapper {

    /**
     * 插入指标数据
     */
    int insert(ReportModelIndicator indicator);

    /**
     * 根据报告ID查询指标列表
     */
    List<ReportModelIndicator> selectByReportId(@Param("reportId") Long reportId);

    /**
     * 根据模型结果ID查询指标列表
     */
    List<ReportModelIndicator> selectByModelResultId(@Param("modelResultId") Long modelResultId);

    /**
     * 根据报告ID删除指标数据
     */
    int deleteByReportId(@Param("reportId") Long reportId);

    /**
     * 联查获取指标详情（包含单位、参考范围）
     * 查询优先级最高的模型的指标数据
     */
    List<ReportModelIndicator> selectIndicatorsWithDetails(@Param("reportId") Long reportId,
                                                           @Param("gender") Integer gender,
                                                           @Param("age") Integer age);

    /**
     * 根据报告ID和模型ID查询指标列表
     */
    List<ReportModelIndicator> selectByReportIdAndModelId(@Param("reportId") Long reportId,
                                                          @Param("modelId") Long modelId);
}

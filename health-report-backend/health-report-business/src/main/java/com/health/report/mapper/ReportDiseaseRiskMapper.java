package com.health.report.mapper;

import com.health.report.domain.ReportDiseaseRisk;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 疾病风险结果Mapper
 */
public interface ReportDiseaseRiskMapper {

    /**
     * 插入疾病风险结果
     */
    int insert(ReportDiseaseRisk risk);

    /**
     * 批量插入疾病风险结果
     */
    int batchInsert(@Param("list") List<ReportDiseaseRisk> risks);

    /**
     * 根据报告ID查询疾病风险列表
     */
    List<ReportDiseaseRisk> selectByReportId(@Param("reportId") Long reportId);

    /**
     * 根据报告ID和模型ID查询
     */
    ReportDiseaseRisk selectByReportIdAndModelId(@Param("reportId") Long reportId,
                                                 @Param("modelId") Long modelId);

    /**
     * 根据报告ID删除疾病风险数据
     */
    int deleteByReportId(@Param("reportId") Long reportId);
}

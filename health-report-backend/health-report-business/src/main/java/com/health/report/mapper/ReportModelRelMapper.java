package com.health.report.mapper;

import com.health.report.domain.ReportModelRel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReportModelRelMapper {
    /**
     * 批量插入关联关系
     */
    int insertBatch(@Param("list") List<ReportModelRel> list);
}

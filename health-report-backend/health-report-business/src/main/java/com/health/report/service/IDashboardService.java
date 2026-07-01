package com.health.report.service;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘Service接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface IDashboardService {
    /**
     * 看板统计
     *
     * @param timeRange 时间范围
     * @return 统计数据
     */
    Map<String, Object> stats(String timeRange);

    /**
     * 概览数据
     *
     * @param timeRange 时间范围
     * @return 概览数据
     */
    Map<String, Object> overview(String timeRange);

    /**
     * 用户趋势
     *
     * @param timeRange 时间范围
     * @return 用户趋势数据
     */
    List<Map<String, Object>> userTrend(String timeRange);

    /**
     * 报告趋势
     *
     * @param timeRange 时间范围
     * @return 报告趋势数据
     */
    List<Map<String, Object>> reportTrend(String timeRange);

    /**
     * 模型使用情况
     *
     * @param timeRange 时间范围
     * @return 模型使用数据
     */
    List<Map<String, Object>> modelUsage(String timeRange);
}

package com.health.report.controller;

import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.service.IDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘Controller
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@RestController
@RequestMapping("/healthReport/dashboard")
public class DashboardController extends BaseController {

    @Autowired
    private IDashboardService dashboardService;

    /**
     * 看板统计
     */
    @GetMapping("/stats")
    public AjaxResult stats(@RequestParam String timeRange) {
        return success(dashboardService.stats(timeRange));
    }

    /**
     * 概览数据
     */
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam String timeRange) {
        return success(dashboardService.overview(timeRange));
    }

    /**
     * 用户趋势
     */
    @GetMapping("/user-trend")
    public AjaxResult userTrend(@RequestParam String timeRange) {
        return success(dashboardService.userTrend(timeRange));
    }

    /**
     * 报告趋势
     */
    @GetMapping("/report-trend")
    public AjaxResult reportTrend(@RequestParam String timeRange) {
        return success(dashboardService.reportTrend(timeRange));
    }

    /**
     * 模型使用情况
     */
    @GetMapping("/model-usage")
    public AjaxResult modelUsage(@RequestParam String timeRange) {
        return success(dashboardService.modelUsage(timeRange));
    }
}

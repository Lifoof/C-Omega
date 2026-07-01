package com.health.report.service.impl;

import com.health.report.common.core.domain.model.LoginUser;
import com.health.report.common.utils.SecurityUtils;
import com.health.report.domain.AiModel;
import com.health.report.domain.CollectionRecord;
import com.health.report.domain.MemberInfo;
import com.health.report.domain.Report;
import com.health.report.mapper.AiModelMapper;
import com.health.report.mapper.CollectionRecordMapper;
import com.health.report.mapper.MemberInfoMapper;
import com.health.report.mapper.ReportMapper;
import com.health.report.service.IDashboardService;
import com.health.report.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘Service业务层处理
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@Service
public class DashboardServiceImpl implements IDashboardService {

    @Autowired
    private MemberInfoMapper memberInfoMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private CollectionRecordMapper collectionRecordMapper;

    @Autowired
    private AiModelMapper aiModelMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private boolean isAdmin() {
        return SecurityUtils.isAdmin(SecurityUtils.getLoginUser().getUserId());
    }

    @Override
    public Map<String, Object> stats(String timeRange) {
        if (!isAdmin()) {
            return Map.of("totalUsers", 0L, "totalReports", 0L, "totalModelCalls", 0L, "reportsByDay", List.of());
        }

        int days = getDays(timeRange);
        Date startDate = Date.from(LocalDateTime.now().minusDays(days).atZone(ZoneId.systemDefault()).toInstant());

        long totalUsers = memberInfoMapper.selectCount(null);
        long totalReports = reportMapper.selectCount(null);
        List<AiModel> models = aiModelMapper.selectList(new AiModel());
        long totalModelCalls = models.stream().mapToLong(m -> (m.getCallCount() != null ? m.getCallCount() : 0)).sum();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<Report> recent = reportMapper.selectReportList(new Report());
        Map<String, Long> byDay = recent.stream()
                .filter(r -> r.getCreateTime() != null && r.getCreateTime().after(startDate))
                .collect(Collectors.groupingBy(r -> sdf.format(r.getCreateTime()),
                        Collectors.counting()));

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("totalReports", totalReports);
        result.put("totalModelCalls", totalModelCalls);
        result.put("reportsByDay", byDay);
        return result;
    }

    private int getDays(String timeRange) {
        return switch (timeRange) {
            default -> 30;
            case "quarter" -> 90;
            case "year" -> 365;
            case "week" -> 7;
        };
    }

    @Override
    public Map<String, Object> overview(String timeRange) {
        int days = getDays(timeRange);
        Date startDate = Date.from(LocalDateTime.now().minusDays(days).atZone(ZoneId.systemDefault()).toInstant());

        if (!isAdmin()) {
            long totalUsers = 0;
            Report report=new Report();
            report.setUserId(SecurityUtils.getLoginUser().getUserId());
            report.setStartDate(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
            long totalReports = reportMapper.selectCount(report);
            CollectionRecord collectionRecord=new CollectionRecord();
            collectionRecord.setUserId(SecurityUtils.getLoginUser().getUserId());
            collectionRecord.setStartDate(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
            long totalCollections = collectionRecordMapper.selectCount(collectionRecord);
            // 1. 普通用户：传自己ID
            Map<String, Object> modelMap = reportMapper.selectMostUsedModel( SecurityUtils.getLoginUser().getUserId(), startDate);

            String mostUsedModelName = "";
            String mostUsedModelNameEn = "";
            int mostUsedModelCount = 0;

            if (modelMap != null) {
                mostUsedModelName = (String) modelMap.get("modelName");
                mostUsedModelNameEn = (String) modelMap.get("modelNameEn");
                mostUsedModelCount = ((Long) modelMap.get("useCount")).intValue();
            }
            Map<String, Object> result = new HashMap<>();
            result.put("totalUsers", totalUsers);
            result.put("totalReports", totalReports);
            result.put("totalCollections", totalCollections);
            result.put("mostUsedModel", mostUsedModelName);
            result.put("mostUsedModelEn", mostUsedModelNameEn);
            result.put("mostUsedModelCount", mostUsedModelCount);
            return result;
        }
        MemberInfo memberInfo=new MemberInfo();
        memberInfo.setStartDate(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        long totalUsers = memberInfoMapper.selectCount(memberInfo);
        Report report=new Report();
        report.setStartDate(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        long totalReports = reportMapper.selectCount(report);
        CollectionRecord collectionRecord=new CollectionRecord();
        collectionRecord.setStartDate(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        long totalCollections = collectionRecordMapper.selectCount(collectionRecord);
        AiModel aiModel=new AiModel();
        aiModel.setStartDate(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        List<AiModel> models = aiModelMapper.selectList(aiModel);
        AiModel mostUsed = models.stream()
                .max(Comparator.comparingInt(m -> m.getCallCount() != null ? m.getCallCount() : 0))
                .orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("totalReports", totalReports);
        result.put("totalCollections", totalCollections);
        result.put("mostUsedModel", mostUsed != null ? mostUsed.getModelName() : "");
        result.put("mostUsedModelEn", mostUsed != null ? mostUsed.getModelNameEn() : "");
        result.put("mostUsedModelCount", mostUsed != null && mostUsed.getCallCount() != null ? mostUsed.getCallCount() : 0);
        return result;
    }

    @Override
    public List<Map<String, Object>> userTrend(String timeRange) {
        if (!isAdmin()) return List.of();

        int days = getDays(timeRange);
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        List<Map<String, Object>> userStats = sysUserMapper.selectUserRegisterStats(start);

        return userStats.stream().map(map -> {
            Map<String, Object> result = new HashMap<>();
            result.put("date", map.get("date"));
            result.put("count", map.get("count"));
            return result;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> reportTrend(String timeRange) {
        Report report = new Report();
        if (!isAdmin()) {
            report.setUserId(SecurityUtils.getLoginUser().getUserId());
        }
        int days = getDays(timeRange);
        Date startDate = Date.from(LocalDateTime.now().minusDays(days).atZone(ZoneId.systemDefault()).toInstant());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<Report> list = reportMapper.selectReportList(report);
        return list.stream()
                .filter(r -> r.getCreateTime() != null && r.getCreateTime().after(startDate))
                .collect(Collectors.groupingBy(r -> sdf.format(r.getCreateTime())))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Map.<String, Object>of("date", e.getKey(), "count", e.getValue().size()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> modelUsage(String timeRange) {
        // 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser.getUserId();
        boolean isAdmin = SecurityUtils.isAdmin(SecurityUtils.getLoginUser().getUserId());
        int days = getDays(timeRange);
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        // 1. 普通用户只能看自己的；管理员看全部
        List<Map<String, Object>> modelStats = reportMapper.selectModelUseStats(isAdmin ? null : userId, start);

        // 2. 计算总次数
        long total = modelStats.stream()
                .mapToLong(map -> ((Number) map.get("count")).longValue())
                .sum();

        // 3. 计算百分比（和你原来格式完全一样）
        return modelStats.stream().map(map -> {
            long count = ((Number) map.get("count")).longValue();
            double percentage = total > 0 ? (double) count / total * 100 : 0;

            Map<String, Object> result = new HashMap<>();
            result.put("modelName", map.get("modelName"));
            result.put("modelNameEn", map.get("modelNameEn"));
            result.put("count", count);
            result.put("percentage", percentage);
            return result;
        }).collect(Collectors.toList());
    }
}

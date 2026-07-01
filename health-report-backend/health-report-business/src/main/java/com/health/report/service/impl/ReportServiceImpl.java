package com.health.report.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.report.common.constant.AppConstants;
import com.health.report.common.exception.BusinessException;
import com.health.report.common.exception.ServiceException;
import com.health.report.common.result.ResultCode;
import com.health.report.common.utils.DateUtils;
import com.health.report.common.utils.SecurityUtils;
import com.health.report.domain.*;
import com.health.report.mapper.*;
import com.health.report.service.IReportService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.health.report.common.utils.SecurityUtils.isAdmin;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private CollectionRecordMapper collectionRecordMapper;
    @Autowired
    private CollectionDataItemMapper collectionDataItemMapper;
    @Autowired
    private AiModelMapper aiModelMapper;
    @Autowired
    private MemberInfoMapper memberInfoMapper;
    @Autowired
    private ReportAsyncService reportAsyncService;
    @Autowired
    private ReportModelRelMapper reportModelRelMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 状态常量（生产环境禁止魔法数字）
    private static final int REPORT_STATUS_GENERATING = 0;
    private static final int COLLECTION_REPORT_GENERATING = 1;

    /**
     * 查询健康报告
     */
    @Override
    public Report selectReportById(Long id) {
        Report report = reportMapper.selectReportById(id);
        if (report == null) {
            throw new ServiceException("报告不存在");
        }
        if (!isAdmin() && !report.getUserId().equals(SecurityUtils.getUserId())) {
            throw new ServiceException("无权访问此报告");
        }
        return report;
    }

    /**
     * 查询健康报告列表
     */
    @Override
    public List<Report> selectReportList(Report report) {
        if (!isAdmin()) {
            report.setUserId(SecurityUtils.getUserId());
        }
        return reportMapper.selectReportList(report);
    }

    /**
     * 新增健康报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertReport(Report report) {
        report.setUserId(SecurityUtils.getUserId());
        String requestId = UUID.randomUUID().toString().replace("-", "");
        report.setReportNo(AppConstants.REPORT_NO_PREFIX + requestId.substring(0, 12).toUpperCase());
        return reportMapper.insertReport(report);
    }

    /**
     * 修改健康报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateReport(Report report) {
        report.setUpdateTime(DateUtils.getNowDate());
        report.setUpdateBy(SecurityUtils.getLoginUser().getUsername());
        return reportMapper.updateReport(report);
    }

    /**
     * 批量删除健康报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteReportByIds(Long[] ids) {
        return reportMapper.deleteReportByIds(ids);
    }

    @Override
    public int deleteReportById(Long id) {
        return reportMapper.deleteReportById(id);
    }

    /**
     * 批量生成报告（生产高并发稳定版）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Report> generateReportBatch(Long collectionId, List<Long> modelIds) {
        List<Report> reportList = new ArrayList<>(1);

        // 1. 基础校验
        CollectionRecord record = collectionRecordMapper.selectById(collectionId);
        if (record == null) {
            throw new BusinessException(ResultCode.COLLECTION_NOT_FOUND);
        }

        // 权限校验
        if (!isAdmin() && !record.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        MemberInfo member = memberInfoMapper.selectById(record.getMemberId());
        Map<String, Object> allFieldData = loadAllFieldData(collectionId);

        // 2. 查询有效模型 + 去重（高并发必备）
        if (CollectionUtils.isEmpty(modelIds)) {
            throw new BusinessException("未选择评估模型");
        }

        List<AiModel> validModelList = aiModelMapper.selectBatchIds(modelIds).stream()
                .filter(Objects::nonNull)
                .filter(model -> model.getStatus() != null && model.getStatus() == 1)
                .distinct()
                .collect(Collectors.toList());

        if (validModelList.isEmpty()) {
            throw new BusinessException("未选择有效的评估模型");
        }

        // 3. 计算参数完整度
        Set<String> allRequiredCodes = new HashSet<>();
        for (AiModel model : validModelList) {
            List<String> requiredParams = parseRequiredParams(model.getAllParams());
            if (!CollectionUtils.isEmpty(requiredParams)) {
                allRequiredCodes.addAll(requiredParams);
            }
        }

        long filledCount = allRequiredCodes.stream().filter(allFieldData::containsKey).count();
        BigDecimal completeness = BigDecimal.ZERO;
        if (!allRequiredCodes.isEmpty()) {
            completeness = BigDecimal.valueOf(filledCount)
                    .divide(BigDecimal.valueOf(allRequiredCodes.size()), 4, RoundingMode.HALF_UP);
        } else {
            completeness = BigDecimal.ONE;
        }

        // 4. 拼接模型名称
        String modelNames = validModelList.stream()
                .map(AiModel::getModelName)
                .collect(Collectors.joining(" + "));

        String modelNameEns = validModelList.stream()
                .map(AiModel::getModelNameEn)
                .collect(Collectors.joining(" + "));

        // 5. 插入报告主记录
        String uuid = UUID.randomUUID().toString().replace("-", "");
        Report report = new Report();
        report.setUserId(record.getUserId());
        report.setMemberId(record.getMemberId());
        report.setCollectionId(collectionId);
        report.setModelId(0L);
        report.setReportNo(AppConstants.REPORT_NO_PREFIX + uuid.substring(0, 12).toUpperCase());
        report.setMemberName(member != null ? member.getName() : record.getMemberName());
        report.setModelName(modelNames);
        report.setModelNameEn(modelNameEns);
        report.setCompleteness(completeness.setScale(4, RoundingMode.HALF_UP));
        report.setStatus(REPORT_STATUS_GENERATING);
        report.setGenerationTime(new Date());

        reportMapper.insertReport(report);
        Long reportId = report.getId();

        // 6. 批量插入关联关系
        List<ReportModelRel> relList = new ArrayList<>(validModelList.size());
        for (AiModel model : validModelList) {
            ReportModelRel rel = new ReportModelRel();
            rel.setReportId(reportId);
            rel.setModelId(model.getId());
            rel.setCreateTime(new Date());
            rel.setUpdateTime(new Date());
            relList.add(rel);
        }

        if (!relList.isEmpty()) {
            reportModelRelMapper.insertBatch(relList);
        }

        // 7. 原子更新模型调用次数（高并发安全，修复计数丢失）
        for (AiModel model : validModelList) {
            aiModelMapper.incrementCallCount(model.getId());
        }

        // 8. 更新采集状态为生成中
        record.setReportStatus(COLLECTION_REPORT_GENERATING);
        collectionRecordMapper.updateById(record);

        // 9. 异步执行模型
        reportAsyncService.asyncExecuteModels(reportId, collectionId, validModelList);

        reportList.add(report);
        return reportList;
    }

    /**
     * 加载采集项数据（空值安全）
     */
    private Map<String, Object> loadAllFieldData(Long collectionId) {
        List<CollectionDataItem> dataList = collectionDataItemMapper.selectByCollectionId(collectionId);
        Map<String, Object> result = new LinkedHashMap<>();

        for (CollectionDataItem item : dataList) {
            Object value = item.getNumberValue() != null ? item.getNumberValue() : item.getStringValue();
            if (value == null || value.toString().isBlank()) {
                continue;
            }
            result.put(item.getFieldCode(), value);
        }
        return result;
    }

    /**
     * 解析必填参数
     */
    private List<String> parseRequiredParams(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void deleteById(Long id) {
        getById(id);
        reportMapper.deleteReportById(id);
    }

    public Report getById(Long id) {
        Report r = reportMapper.selectReportById(id);
        if (r == null) {
            throw new BusinessException(ResultCode.REPORT_NOT_FOUND);
        }
        if (!isAdmin() && !r.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return r;
    }
}
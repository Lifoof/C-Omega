package com.health.report.service.impl;

import com.health.report.domain.*;
import com.health.report.mapper.*;
import com.health.report.utils.PythonScriptClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportAsyncService {

    @Autowired
    private PythonScriptClient pythonScriptClient;
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private CollectionFieldConfigMapper collectionFieldConfigMapper;
    @Autowired
    private CollectionDataItemMapper collectionDataItemMapper;
    @Autowired
    private CollectionRecordMapper collectionRecordMapper;
    @Autowired
    private MemberInfoMapper memberInfoMapper;
    @Autowired
    private HealthReportPdfServiceImpl healthReportPdfService;

    @Qualifier("modelCallExecutor")
    private final Executor modelCallExecutor;

    // 全局超时（防止Python卡死）
    private static final long MODEL_TIMEOUT_SECONDS = 240L;

    // 状态常量
    private static final int REPORT_SUCCESS = 1;
    private static final int REPORT_FAILED = 2;
    private static final int COLLECTION_FINISH_SUCCESS = 1;
    private static final int COLLECTION_REPORT_SUCCESS = 2;
    private static final int COLLECTION_FINISH_FAILED = 0;
    private static final int COLLECTION_REPORT_FAILED = 3;

    /**
     * 顶层异步入口：使用 reportGenerateExecutor 线程池
     */
    @Async("reportGenerateExecutor")
    public void asyncExecuteModels(Long reportId, Long collectionId, List<AiModel> validModelList) {
        log.info("[报告生成-启动] reportId:{}, 模型数量:{}", reportId, validModelList.size());

        if (CollectionUtils.isEmpty(validModelList)) {
            log.error("[报告生成-失败] 无有效模型 reportId:{}", reportId);
            updateFailStatus(reportId, collectionId);
            return;
        }

        try {
            // 并发执行模型
            executeModelsParallel(reportId, collectionId, validModelList);

            // 后处理（全部容错）
            doPostProcess(reportId, collectionId);

            // 更新成功状态
            updateSuccessStatus(reportId, collectionId);
            log.info("[报告生成-完成] reportId:{}", reportId);

        } catch (Exception e) {
            log.error("[报告生成-异常] reportId:{}", reportId, e);
            updateFailStatus(reportId, collectionId);
        }
    }

    /**
     * 模型并行执行（使用 modelCallExecutor 高并发线程池）
     */
    private void executeModelsParallel(Long reportId, Long collectionId, List<AiModel> validModelList) {
        List<CompletableFuture<Void>> futures = validModelList.stream()
                .map(model -> CompletableFuture.runAsync(() -> {
                    log.info("[模型开始] ID:{}, 名称:{}", model.getId(), model.getModelName());
                    Map<String, Object> params = buildModelParams(model.getId(), collectionId);

                    pythonScriptClient.callModel(
                            reportId,
                            model.getId(),
                            params,
                            model.getPyPath(),
                            model.getModelName(),
                            model.getPriority(),
                            model.getCategory()
                    );

                    log.info("[模型完成] ID:{}, 名称:{}", model.getId(), model.getModelName());
                }, modelCallExecutor))
                .collect(Collectors.toList());

        // 等待全部完成 + 全局超时熔断
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(MODEL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }

    /**
     * 后处理逻辑（全部容错，不影响主流程）
     */
    private void doPostProcess(Long reportId, Long collectionId) {
        // 1. 清理整体情况模型结果
        try {
            pythonScriptClient.cleanupWholeModelResults(reportId);
        } catch (Exception e) {
            log.warn("[清理整体模型失败] reportId:{}", reportId, e);
        }

        // 2. 填充缺失系统类型
        try {
            CollectionRecord record = collectionRecordMapper.selectById(collectionId);
            MemberInfo member = memberInfoMapper.selectById(record.getMemberId());
            BigDecimal actualAge = new BigDecimal(pythonScriptClient.calculateAge(member));
            pythonScriptClient.fillMissingSystemTypes(reportId, actualAge);
        } catch (Exception e) {
            log.warn("[填充系统类型失败] reportId:{}", reportId, e);
        }

        // 3. 填充缺失疾病类型
        try {
            pythonScriptClient.fillMissingDiseaseTypes(reportId);
        } catch (Exception e) {
            log.warn("[填充疾病类型失败] reportId:{}", reportId, e);
        }

        // 4. PDF生成（独立容错）
        try {
            healthReportPdfService.generatePdfReport(reportId);
            log.info("[PDF生成成功] reportId:{}", reportId);
        } catch (Exception e) {
            log.error("[PDF生成失败] reportId:{}", reportId, e);
        }
    }

    /**
     * 构建模型参数（空值安全，不传 NaN）
     */
    private Map<String, Object> buildModelParams(Long modelId, Long collectionId) {
        List<CollectionFieldConfig> fields = collectionFieldConfigMapper.selectParamsByModelId(modelId);
        List<Map<String, Object>> items = collectionDataItemMapper.selectValuesByCollectionId(collectionId);

        Map<String, Object> valueMap = new HashMap<>(items.size());
        for (Map<String, Object> item : items) {
            valueMap.put(item.get("fieldCode").toString(), item.get("fieldValue"));
        }

        Map<String, Object> params = new LinkedHashMap<>();
        for (CollectionFieldConfig field : fields) {
            params.put(field.getFieldParamName(), valueMap.get(field.getFieldCode()));
        }
        return params;
    }

    // ====================== 状态更新工具方法 ======================
    private void updateSuccessStatus(Long reportId, Long collectionId) {
        updateReportStatus(reportId, REPORT_SUCCESS);
        updateCollectionStatus(collectionId, COLLECTION_FINISH_SUCCESS, COLLECTION_REPORT_SUCCESS);
    }

    private void updateFailStatus(Long reportId, Long collectionId) {
        updateReportStatus(reportId, REPORT_FAILED);
        updateCollectionStatus(collectionId, COLLECTION_FINISH_FAILED, COLLECTION_REPORT_FAILED);
    }

    private void updateReportStatus(Long reportId, int status) {
        try {
            Report up = new Report();
            up.setId(reportId);
            up.setStatus(status);
            reportMapper.updateReport(up);
        } catch (Exception e) {
            log.error("更新报告状态异常 reportId:{}", reportId, e);
        }
    }

    private void updateCollectionStatus(Long collectionId, int finishStatus, int reportStatus) {
        try {
            CollectionRecord record = collectionRecordMapper.selectById(collectionId);
            if (record == null) return;

            record.setFinishStatus(finishStatus);
            record.setReportStatus(reportStatus);
            collectionRecordMapper.updateById(record);
        } catch (Exception e) {
            log.error("更新采集记录异常 collectionId:{}", collectionId, e);
        }
    }
}
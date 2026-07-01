package com.health.report.service;

import com.health.report.dto.collection.excel.BatchImportRequest;
import com.health.report.dto.collection.excel.BatchImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 采集信息批量导入Service
 */
public interface CollectionBatchImportService {

    /**
     * 多文件批量导入
     * @param request 导入请求（多文件+覆盖开关）
     * @return 导入结果（成功数、失败数、失败原因）
     */
    BatchImportResponse batchImport(BatchImportRequest request);

    /**
     * 单个文件导入（供多文件调用）
     * @param file 单个Excel文件
     * @param overrideIfExist 是否覆盖已存在记录
     * @param failReasons 失败详情列表（用于收集多个文件的失败信息）
     * @param collectionIds 成功的collection_id列表（用于收集结果）
     */
    void importSingleFile(MultipartFile file, boolean overrideIfExist,
                          List<BatchImportResponse.ImportFailDetail> failReasons,
                          List<Long> collectionIds);
}

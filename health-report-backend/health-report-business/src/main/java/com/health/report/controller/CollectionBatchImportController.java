package com.health.report.controller;

import com.health.report.common.annotation.Log;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.enums.BusinessType;
import com.health.report.dto.collection.excel.BatchImportRequest;
import com.health.report.dto.collection.excel.BatchImportResponse;
import com.health.report.service.CollectionBatchImportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.health.report.common.core.domain.AjaxResult.success;

/**
 * 采集信息批量导入Controller
 */
@RestController
@RequestMapping("/healthReport/collection")
public class CollectionBatchImportController {

    @Autowired
    private CollectionBatchImportService batchImportService;

    /**
     * 批量导入接口（支持多文件）
     * @param files 多Excel文件
     * @param languageType 语音
     * @return 导入结果
     */
    @PostMapping("/batchImport")
    @Log(title = "采集信息批量导入", businessType = BusinessType.INSERT)
    public AjaxResult batchImport(List<MultipartFile> files,@RequestParam(required = false, defaultValue = "zh") String languageType) {
        BatchImportRequest request = new BatchImportRequest();
        request.setFiles(files);
        request.setOverrideIfExist(false);
        request.setLanguageType(languageType);
        BatchImportResponse response = batchImportService.batchImport(request);

        return success(response);
    }

}

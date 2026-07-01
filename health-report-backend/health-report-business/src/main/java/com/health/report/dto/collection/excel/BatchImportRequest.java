package com.health.report.dto.collection.excel;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 批量导入接口请求DTO
 */
@Data
public class BatchImportRequest {
    // 多Excel文件
    private List<MultipartFile> files;

    // 已存在记录是否覆盖（可选，默认false=跳过）
    private Boolean overrideIfExist = false;

    // 语言类型：zh=中文, en=英文
    private String languageType = "zh";
}

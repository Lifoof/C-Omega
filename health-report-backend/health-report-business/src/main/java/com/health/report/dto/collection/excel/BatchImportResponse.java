package com.health.report.dto.collection.excel;

import lombok.Data;
import java.util.List;

/**
 * 批量导入接口响应DTO
 */
@Data
public class BatchImportResponse {
    // 成功导入的主表记录数
    private Integer successCount = 0;

    // 失败的主表记录数
    private Integer failCount = 0;

    // 失败详情（行号、成员名、原因）
    private List<ImportFailDetail> failReasons;

    // 成功生成的collection_id列表
    private List<Long> collectionIds;

    /**
     * 导入失败详情内部类
     */
    @Data
    public static class ImportFailDetail {
        // Excel文件名称
        private String fileName;

        // Excel主表行号
        private Integer rowNum;

        // 成员姓名
        private String memberName;

        // 失败原因
        private String reason;
    }
}

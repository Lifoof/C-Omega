package com.health.report.dto.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiModelDTO {
    private Long id;
    private String modelName;
    private String modelNameEn;
    private String modelCode;
    private String category;
    private Integer applicableGender;
    private String description;
    private String requiredParams; // JSON array string
    private Integer requiredParamCount;
    private String allParams; // JSON array string
    private Integer allParamCount;
    private Integer paramCount;
    private Integer status;
    private String pyPath;
    private String pklPath;
    /** 优先级：数字越小优先级越高 */
    private Integer priority;
}

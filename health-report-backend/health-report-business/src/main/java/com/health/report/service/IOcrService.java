package com.health.report.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * OCR服务接口
 *
 * @author health-report
 */
public interface IOcrService {

    /**
     * 单文件OCR识别
     *
     * @param file 图片或PDF文件
     * @return 识别文本
     */
    String recognize(MultipartFile file) throws Exception;

    /**
     * 批量OCR识别
     *
     * @param files 文件列表
     * @return 识别结果列表
     */
    List<Map<String, Object>> recognizeBatch(List<MultipartFile> files) throws Exception;

    /**
     * OCR识别并提取体检指标
     *
     * @param files 文件列表
     * @param collectionId 采集记录ID（可选，用于匹配字段配置）
     * @return 提取的指标数据
     */
    Map<String, Object> extractIndicators(List<MultipartFile> files, Long collectionId) throws Exception;

    String getEngineName();
}

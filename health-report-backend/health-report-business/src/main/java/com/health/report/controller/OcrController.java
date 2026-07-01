package com.health.report.controller;

import com.health.report.common.config.RuoYiConfig;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.utils.DateUtils;
import com.health.report.common.utils.StringUtils;
import com.health.report.common.utils.file.FileUploadUtils;
import com.health.report.common.utils.file.ImageCompressUtils;
import com.health.report.common.utils.file.MimeTypeUtils;
import com.health.report.config.OcrProperties;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.framework.config.ServerConfig;
import com.health.report.mapper.CollectionFieldConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/**
 * OCR识别控制器
 * 规则：按行空格分割 → 第一段=指标名，第二段=指标值
 * 保存规则：
 * 1. 图片识别成功 → 保存压缩图
 * 2. 图片识别失败 → 保存原图
 * 3. PDF 始终保存原文件
 * @author health-report
 */
@RestController
@RequestMapping("/healthReport/ocr")
public class OcrController {

    private static final Logger log = LoggerFactory.getLogger(OcrController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OcrProperties ocrProperties;

    @Autowired
    @Qualifier("ocrTaskExecutor")
    private Executor ocrTaskExecutor;

    @Autowired
    private CollectionFieldConfigMapper fieldConfigMapper;

    @Autowired
    private ServerConfig serverConfig;

    /**
     * OCR识别并提取体检指标
     */
    @PostMapping("/extractIndicators")
    public AjaxResult extractIndicators(@RequestParam("files") List<MultipartFile> files,
                                        @RequestParam(value = "collectionId", required = false) Long collectionId) {
        if (files == null || files.isEmpty()) {
            return AjaxResult.error("请选择文件");
        }

        try {
            List<MultipartFile> validFiles = files.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            if (validFiles.isEmpty()) {
                return AjaxResult.error("请选择有效的文件");
            }

            // 1. 预处理所有文件（压缩图片、区分PDF）
            List<ProcessedFile> processedFiles = new ArrayList<>();
            for (MultipartFile file : validFiles) {
                processedFiles.add(processFile(file));
            }

            // 2. 调用OCR（传压缩图）
            List<Map<String, Object>> recognizeResults = Collections.emptyList();
            boolean ocrSuccess = false;
            try {
                recognizeResults = callOcrApiWithProcessedFiles(processedFiles);
                ocrSuccess = true;
            } catch (Exception e) {
                log.error("OCR服务调用失败，将保存原图", e);
                ocrSuccess = false;
            }

            // 3. 匹配配置
            List<CollectionFieldConfig> fieldConfigs = fieldConfigMapper.selectCollectionFieldConfigList(null);
            Map<String, CollectionFieldConfig> fieldMap = new HashMap<>();
            for (CollectionFieldConfig config : fieldConfigs) {
                String key = config.getFieldName().trim().toLowerCase();
                fieldMap.put(key, config);
                if (StringUtils.isNotBlank(config.getFieldNameEn())) {
                    fieldMap.put(config.getFieldNameEn().trim().toLowerCase(), config);
                }
                if (StringUtils.isNotBlank(config.getAliasName())) {
                    for (String alias : config.getAliasName().split("[,，、]")) {
                        fieldMap.put(alias.trim().toLowerCase(), config);
                    }
                }
                if (StringUtils.isNotBlank(config.getAliasNameEn())) {
                    for (String alias : config.getAliasNameEn().split("[,，、;]")) {
                        fieldMap.put(alias.trim().toLowerCase(), config);
                    }
                }
            }

            // 4. 解析结果
            Map<String, Object> extractedData = new HashMap<>();
            List<Map<String, Object>> matchedIndicators = new ArrayList<>();
            if (ocrSuccess && recognizeResults != null) {
                for (Map<String, Object> result : recognizeResults) {
                    if (!(Boolean) result.getOrDefault("success", false)) continue;
                    String text = (String) result.get("text");
                    if (text == null) continue;

                    String[] lines = text.split("\\r?\\n");
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        String[] parts = line.split("\\s+");
                        if (parts.length < 2) continue;

                        String fieldName = parts[0].trim();
                        String valueStr = parts[1].trim();

                        CollectionFieldConfig config = fieldMap.get(fieldName.toLowerCase());
                        if (config == null) continue;

                        BigDecimal value = extractNumber(valueStr);
                        if (value == null) continue;

                        Map<String, Object> indicator = new HashMap<>();
                        indicator.put("fieldCode", config.getFieldCode());
                        indicator.put("fieldName", config.getFieldName());
                        indicator.put("value", value);
                        indicator.put("unit", config.getUnit());
                        indicator.put("originalText", line);
                        matchedIndicators.add(indicator);
                        extractedData.put(config.getFieldCode(), value);
                    }
                }
            }

            // ====================== 核心：按规则保存文件 ======================
            List<String> imageUrls = new ArrayList<>();
            for (ProcessedFile file : processedFiles) {
                String url;
                if (file.isImage()) {
                    // 图片：成功存压缩图，失败存原图
                    if (ocrSuccess) {
                        url = saveFile(file.getProcessedBytes(), file, serverConfig.getUrl());
                    } else {
                        url = saveFile(file.getOriginalBytes(), file, serverConfig.getUrl());
                    }
                } else {
                    // PDF：永远存原图
                    url = saveFile(file.getOriginalBytes(), file, serverConfig.getUrl());
                }
                imageUrls.add(url);
            }

            // 返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("extractedData", extractedData);
            response.put("matchedIndicators", matchedIndicators);
            response.put("totalMatched", matchedIndicators.size());
            response.put("imageUrls", imageUrls);
            response.put("ocrSuccess", ocrSuccess);

            return AjaxResult.success("处理完成", response);
        } catch (Exception e) {
            log.error("指标提取失败", e);
            return AjaxResult.error("处理失败: " + e.getMessage());
        }
    }

    // ====================== 保存文件（根据传入bytes决定存什么） ======================
    private String saveFile(byte[] saveBytes, ProcessedFile processedFile, String serverUrl) throws IOException {
        String filePath = RuoYiConfig.getUploadPath() + "/ocr";
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = DateUtils.datePath() + "/" + System.currentTimeMillis() + "." + processedFile.getFormat();
        File desc = FileUploadUtils.getAbsoluteFile(filePath, fileName);
        java.nio.file.Files.write(desc.toPath(), saveBytes);
        return serverUrl + FileUploadUtils.getPathFileName(filePath, fileName);
    }

    // ====================== 工具方法 ======================
    private ProcessedFile processFile(MultipartFile file) throws IOException {
        String ext = FileUploadUtils.getExtension(file);
        boolean isImage = MimeTypeUtils.isImageExtension(ext);
        byte[] originalBytes = file.getBytes();
        byte[] processedBytes = originalBytes;

        if (isImage) {
            try {
                processedBytes = ImageCompressUtils.compressImage(originalBytes, ext);
            } catch (Exception e) {
                log.warn("图片压缩失败，使用原图");
                processedBytes = originalBytes;
            }
        }
        return new ProcessedFile(file.getOriginalFilename(), originalBytes, processedBytes, ext, isImage);
    }

    private List<Map<String, Object>> callOcrApiWithProcessedFiles(List<ProcessedFile> processedFiles) throws Exception {
        Future<List<Map<String, Object>>> future = ((ThreadPoolTaskExecutor) ocrTaskExecutor)
                .submit(() -> callOcrApiWithProcessed(processedFiles));
        return future.get(120, TimeUnit.SECONDS);
    }

    private List<Map<String, Object>> callOcrApiWithProcessed(List<ProcessedFile> processedFiles) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (ProcessedFile f : processedFiles) {
            body.add("files", new ByteArrayResource(f.getProcessedBytes()) {
                @Override
                public String getFilename() { return f.getOriginalFilename(); }
            });
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<List> response = restTemplate.postForEntity(
                ocrProperties.getServerUrl(), new HttpEntity<>(body, headers), List.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Object obj : response.getBody()) {
                list.add((Map<String, Object>) obj);
            }
            return list;
        }
        throw new Exception("OCR调用失败");
    }

    private BigDecimal extractNumber(String text) {
        try {
            return new BigDecimal(text.replaceAll("[^\\d.]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    // ====================== 内部类 ======================
    private static class ProcessedFile {
        private final String originalFilename;
        private final byte[] originalBytes;
        private final byte[] processedBytes;
        private final String format;
        private final boolean isImage;

        public ProcessedFile(String originalFilename, byte[] originalBytes, byte[] processedBytes,
                             String format, boolean isImage) {
            this.originalFilename = originalFilename;
            this.originalBytes = originalBytes;
            this.processedBytes = processedBytes;
            this.format = format;
            this.isImage = isImage;
        }

        public String getOriginalFilename() { return originalFilename; }
        public byte[] getOriginalBytes() { return originalBytes; }
        public byte[] getProcessedBytes() { return processedBytes; }
        public String getFormat() { return format; }
        public boolean isImage() { return isImage; }
    }
}
package com.health.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OCR配置属性类
 * 从application.yml读取OCR相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {

    /**
     * 外部OCR服务地址
     */
    private String serverUrl = "http://203.119.115.25:8081/api/ocr/batch";

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 30000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 120000;

    /**
     * 异步配置
     */
    private Async async = new Async();

    /**
     * PDF配置
     */
    private Pdf pdf = new Pdf();

    @Data
    public static class Async {
        /**
         * 核心线程池大小
         */
        private int corePoolSize = 4;

        /**
         * 最大线程池大小
         */
        private int maxPoolSize = 8;

        /**
         * 队列容量
         */
        private int queueCapacity = 100;

        /**
         * 线程空闲存活时间（秒）
         */
        private int keepAliveSeconds = 60;
    }

    @Data
    public static class Pdf {
        /**
         * PDF渲染DPI
         */
        private int renderDpi = 150;

        /**
         * 最大处理页数
         */
        private int maxPages = 50;

        /**
         * PDF渲染线程数
         */
        private int renderThreads = 4;
    }
}

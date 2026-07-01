package com.health.report.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * OCR配置类 - 调用外部OCR服务
 * 配置RestTemplate和线程池，支持高并发处理
 */
@Configuration
public class OcrConfiguration {

    @Autowired
    private OcrProperties ocrProperties;

    /**
     * RestTemplate - 用于调用外部OCR服务
     * 配置超时时间，支持高并发场景
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    /**
     * 创建HTTP请求工厂，配置超时时间
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(ocrProperties.getConnectTimeout());
        factory.setReadTimeout(ocrProperties.getReadTimeout());
        return factory;
    }

    /**
     * OCR任务执行器 - 用于并行处理OCR识别任务
     * 支持高并发、多线程处理
     */
    @Bean("ocrTaskExecutor")
    public Executor ocrTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 设置核心线程数
        executor.setCorePoolSize(ocrProperties.getAsync().getCorePoolSize());

        // 设置最大线程数
        executor.setMaxPoolSize(ocrProperties.getAsync().getMaxPoolSize());

        // 设置队列容量
        executor.setQueueCapacity(ocrProperties.getAsync().getQueueCapacity());

        // 设置线程空闲存活时间
        executor.setKeepAliveSeconds(ocrProperties.getAsync().getKeepAliveSeconds());

        // 设置线程名前缀
        executor.setThreadNamePrefix("ocr-task-");

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 关闭时等待所有任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}

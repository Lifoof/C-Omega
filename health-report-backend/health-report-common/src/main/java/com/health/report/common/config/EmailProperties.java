package com.health.report.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮箱配置属性类
 * 从配置文件读取阿里云邮件推送相关配置
 *
 * @author health-report
 */
@Component
@ConfigurationProperties(prefix = "email")
public class EmailProperties {

    /**
     * 是否启用邮箱服务
     */
    private boolean enabled = true;

    /**
     * 阿里云AccessKey ID
     */
    private String accessKeyId;

    /**
     * 阿里云AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * 发件人地址
     */
    private String fromAddress;

    /**
     * 发件人昵称
     */
    private String fromAlias;

    /**
     * 阿里云区域ID
     */
    private String regionId = "cn-hangzhou";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromAlias() {
        return fromAlias;
    }

    public void setFromAlias(String fromAlias) {
        this.fromAlias = fromAlias;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }
}
package com.health.report.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信配置属性类
 * 从配置文件读取阿里云短信相关配置
 *
 * @author health-report
 */
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /**
     * 是否启用短信服务
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
     * 阿里云区域ID
     */
    private String regionId = "cn-hangzhou";

    /**
     * 签名名称（在阿里云控制台配置）
     */
    private String signName;

    /**
     * 注册验证码模板ID
     */
    private String registerTemplateCode;

    /**
     * 登录验证码模板ID
     */
    private String loginTemplateCode;

    /**
     * 重置密码验证码模板ID
     */
    private String resetTemplateCode;

    /**
     * 验证码有效期（分钟）
     */
    private int expireMinutes = 5;

    /**
     * 同一号码发送间隔（秒）
     */
    private int sendIntervalSeconds = 60;

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

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getSignName() {
        return signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public String getRegisterTemplateCode() {
        return registerTemplateCode;
    }

    public void setRegisterTemplateCode(String registerTemplateCode) {
        this.registerTemplateCode = registerTemplateCode;
    }

    public String getLoginTemplateCode() {
        return loginTemplateCode;
    }

    public void setLoginTemplateCode(String loginTemplateCode) {
        this.loginTemplateCode = loginTemplateCode;
    }

    public String getResetTemplateCode() {
        return resetTemplateCode;
    }

    public void setResetTemplateCode(String resetTemplateCode) {
        this.resetTemplateCode = resetTemplateCode;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(int expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public int getSendIntervalSeconds() {
        return sendIntervalSeconds;
    }

    public void setSendIntervalSeconds(int sendIntervalSeconds) {
        this.sendIntervalSeconds = sendIntervalSeconds;
    }
}
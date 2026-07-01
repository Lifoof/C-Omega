package com.health.report.common.core.domain.model;

/**
 * 用户手机验证码登录对象
 * 
 * @author ruoyi
 */
public class SmsLoginBody
{
    /**
     * 手机号
     */
    private String phone;

    /**
     * 手机验证码
     */
    private String smsCode;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }
}

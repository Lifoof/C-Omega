package com.health.report.common.utils;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.health.report.common.config.SmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信服务工具类
 * 使用官方推荐的标准SDK（20170525版本）发送短信验证码
 *
 * @author health-report
 */
@Component
public class DySmsHelper {

    private final static Logger logger = LoggerFactory.getLogger(DySmsHelper.class);

    /**产品名称*/
    static final String PRODUCT = "Dysmsapi";
    /**产品域名*/
    static final String DOMAIN = "dysmsapi.aliyuncs.com";

    @Autowired
    private SmsProperties smsProperties;

    /**
     * 发送注册验证码
     *
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendRegisterCode(String phoneNumber, String code) {
        return sendSms(phoneNumber, code, "register");
    }

    /**
     * 发送登录验证码
     *
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendLoginCode(String phoneNumber, String code) {
        return sendSms(phoneNumber, code, "login");
    }

    /**
     * 发送重置密码验证码
     *
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendResetCode(String phoneNumber, String code) {
        return sendSms(phoneNumber, code, "reset");
    }

    /**
     * 发送短信验证码（通用方法）
     *
     * @param phoneNumber 手机号
     * @param code 验证码
     * @param type 场景类型：register/login/reset
     * @return 是否发送成功
     */
    public boolean sendSms(String phoneNumber, String code, String type) {
        // 检查是否启用短信服务
        if (!smsProperties.isEnabled()) {
            logger.warn("短信服务未启用");
            return false;
        }

        // 设置超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");

        try {
            // 初始化AcsClient
            IClientProfile profile = DefaultProfile.getProfile(
                    smsProperties.getRegionId(),
                    smsProperties.getAccessKeyId(),
                    smsProperties.getAccessKeySecret());
            DefaultProfile.addEndpoint(smsProperties.getRegionId(), smsProperties.getRegionId(), PRODUCT, DOMAIN);
            IAcsClient acsClient = new DefaultAcsClient(profile);

            // 构建请求
            SendSmsRequest request = new SendSmsRequest();
            request.setPhoneNumbers(phoneNumber);
            request.setSignName(smsProperties.getSignName());

            // 根据类型选择模板
            String templateCode = getTemplateCode(type);
            if (templateCode == null) {
                logger.error("未知的短信类型: {}", type);
                return false;
            }
            request.setTemplateCode(templateCode);

            // 设置模板参数
            String templateParam = "{\"code\":\"" + code + "\",\"expireMinutes\":\"" + smsProperties.getExpireMinutes() + "\"}";
            request.setTemplateParam(templateParam);

            // 发送短信
            SendSmsResponse response = acsClient.getAcsResponse(request);

            // 检查返回结果
            if ("OK".equals(response.getCode())) {
                logger.info("短信发送成功，类型：{}，手机号：{}，RequestId：{}",
                        type, phoneNumber, response.getRequestId());
                return true;
            } else {
                logger.error("短信发送失败，类型：{}，手机号：{}，错误码：{}，错误信息：{}",
                        type, phoneNumber, response.getCode(), response.getMessage());
                return false;
            }

        } catch (ClientException e) {
            logger.error("短信发送异常，类型：{}，手机号：{}，错误信息：{}",
                    type, phoneNumber, e.getMessage());
            return false;
        }
    }

    /**
     * 根据类型获取模板ID
     */
    private String getTemplateCode(String type) {
        switch (type) {
            case "register":
                return smsProperties.getRegisterTemplateCode();
            case "login":
                return smsProperties.getLoginTemplateCode();
            case "reset":
                return smsProperties.getResetTemplateCode();
            default:
                return smsProperties.getLoginTemplateCode(); // 默认使用登录模板
        }
    }

    /**
     * 生成6位数字验证码
     *
     * @return 6位验证码
     */
    public String generateCode() {
        return String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
    }

    /**
     * 生成指定长度的数字验证码
     *
     * @param length 验证码长度
     * @return 指定长度的验证码
     */
    public String generateCode(int length) {
        if (length <= 0 || length > 10) {
            length = 6;
        }
        int max = (int) Math.pow(10, length) - 1;
        int min = (int) Math.pow(10, length - 1);
        return String.valueOf((int) (Math.random() * (max - min + 1) + min));
    }
}
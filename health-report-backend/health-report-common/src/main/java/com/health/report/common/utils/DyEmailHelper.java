package com.health.report.common.utils;

import com.aliyun.dm20151123.Client;
import com.aliyun.dm20151123.models.SingleSendMailRequest;
import com.aliyun.dm20151123.models.SingleSendMailResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.health.report.common.config.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 阿里云DirectMail邮件推送工具类
 * 使用阿里云邮件推送服务发送邮件验证码
 *
 * @author health-report
 */
@Component
public class DyEmailHelper {

    private final static Logger logger = LoggerFactory.getLogger(DyEmailHelper.class);

    @Autowired
    private EmailProperties emailProperties;

    /**
     * 发送邮件验证码
     *
     * @param toAddress 收件人邮箱地址
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendEmailCode(String toAddress, String code) {
        // 检查是否启用邮箱服务
        if (!emailProperties.isEnabled()) {
            logger.warn("邮箱服务未启用");
            return false;
        }

        try {
            // 初始化客户端
            Client client = createClient();

            // 组装请求
            SingleSendMailRequest request = new SingleSendMailRequest();
            request.setAccountName(emailProperties.getFromAddress());
            request.setFromAlias(emailProperties.getFromAlias());
            request.setAddressType(1);
            request.setToAddress(toAddress);
            request.setSubject("【健康体检报告系统】验证码");
            request.setHtmlBody(buildEmailContent(code));

            // 发送邮件
            SingleSendMailResponse response = client.singleSendMail(request);

            logger.info("邮件发送成功，RequestId: {}, 收件人: {}", response.getBody().getRequestId(), toAddress);
            return true;

        } catch (TeaException e) {
            logger.error("邮件发送失败: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("邮件发送异常", e);
            return false;
        }
    }

    /**
     * 发送通用验证码（注册/重置密码通用）
     * @param toAddress 收件邮箱
     * @param code 验证码
     * @param type 场景类型：register/reset
     * @return 是否成功
     */
    public boolean sendVerifyCode(String toAddress, String code, String type) {
        // 检查是否启用邮箱服务
        if (!emailProperties.isEnabled()) {
            logger.warn("邮箱服务未启用");
            return false;
        }

        try {
            SingleSendMailRequest request = new SingleSendMailRequest();
            request.setAccountName(emailProperties.getFromAddress());
            request.setFromAlias(emailProperties.getFromAlias());
            request.setAddressType(1); // 1=触发邮件（注册/找回密码专用）
            request.setReplyToAddress(false);
            request.setToAddress(toAddress);

            // 根据场景设置不同的标题和内容
            String subject;
            String content;
            if ("register".equals(type)) {
                subject = "【" + emailProperties.getFromAlias() + "】注册验证码";
                content = "您正在注册账号，验证码为：" + code + "，有效期5分钟，请勿泄露。";
            } else if ("reset".equals(type)) {
                subject = "【" + emailProperties.getFromAlias() + "】重置密码验证码";
                content = "您正在重置账号密码，验证码为：" + code + "，有效期5分钟，请勿泄露。";
            } else {
                subject = "【" + emailProperties.getFromAlias() + "】验证码";
                content = "您的验证码为：" + code + "，有效期5分钟，请勿泄露。";
            }

            request.setSubject(subject);
            request.setHtmlBody("<div style='padding:20px;'>" + content + "</div>");

            // 发送邮件
            Client client = createClient();
            client.singleSendMail(request);

            logger.info("验证码邮件发送成功，类型：{}，收件人：{}", type, toAddress);
            return true;

        } catch (Exception e) {
            logger.error("验证码邮件发送失败，类型：{}，收件人：{}", type, toAddress, e);
            return false;
        }
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String code) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>验证码邮件</title>" +
                "</head>" +
                "<body style=\"font-family: 'Microsoft YaHei', Arial, sans-serif; background-color: #f5f5f5; padding: 20px;\">" +
                "<div style=\"max-width: 500px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; padding: 30px; box-shadow: 0 2px 12px rgba(0,0,0,0.1);\">" +
                "<h2 style=\"color: #333333; text-align: center; margin-bottom: 20px;\">【南湖实验室omega】</h2>" +
                "<p style=\"color: #666666; font-size: 14px; line-height: 1.6;\">您好，您正在进行邮箱验证操作，您的验证码是：</p>" +
                "<div style=\"text-align: center; margin: 30px 0;\">" +
                "<span style=\"display: inline-block; padding: 15px 30px; font-size: 28px; font-weight: bold; color: #007bff; background-color: #f8f9fa; border-radius: 6px; letter-spacing: 4px;\">" + code + "</span>" +
                "</div>" +
                "<p style=\"color: #999999; font-size: 12px; text-align: center;\">该验证码5分钟内有效，请及时使用。</p>" +
                "<p style=\"color: #999999; font-size: 12px; text-align: center;\">如非本人操作，请忽略此邮件。</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * 发送通用邮件
     *
     * @param toAddress 收件人邮箱
     * @param subject 邮件主题
     * @param htmlBody 邮件正文（HTML格式）
     * @return 是否发送成功
     */
    public boolean sendEmail(String toAddress, String subject, String htmlBody) {
        if (!emailProperties.isEnabled()) {
            logger.warn("邮箱服务未启用");
            return false;
        }

        try {
            Client client = createClient();

            SingleSendMailRequest request = new SingleSendMailRequest();
            request.setAccountName(emailProperties.getFromAddress());
            request.setFromAlias(emailProperties.getFromAlias());
            request.setAddressType(1);
            request.setToAddress(toAddress);
            request.setSubject(subject);
            request.setHtmlBody(htmlBody);

            SingleSendMailResponse response = client.singleSendMail(request);

            logger.info("邮件发送成功，RequestId: {}, 收件人: {}, 主题: {}",
                    response.getBody().getRequestId(), toAddress, subject);
            return true;

        } catch (TeaException e) {
            logger.error("邮件发送失败: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("邮件发送异常", e);
            return false;
        }
    }

    /**
     * 创建邮件客户端
     */
    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(emailProperties.getAccessKeyId())
                .setAccessKeySecret(emailProperties.getAccessKeySecret());
        config.endpoint = "dm.aliyuncs.com";
        return new Client(config);
    }
}
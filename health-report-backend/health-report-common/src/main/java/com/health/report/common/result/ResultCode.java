package com.health.report.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),

    PARAM_ERROR(400, "参数错误"),
    PARAM_MISSING(400, "缺少必要参数"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    VERIFICATION_CODE_ERROR(1004, "验证码错误或已过期"),
    VERIFICATION_CODE_FREQUENT(1005, "验证码发送过于频繁"),
    ACCOUNT_DISABLED(1006, "账户已禁用"),

    MEMBER_NOT_FOUND(2001, "成员不存在"),
    MEMBER_HAS_REPORT(2002, "该成员存在关联报告，无法删除"),

    COLLECTION_NOT_FOUND(3001, "采集记录不存在"),
    COLLECTION_HAS_REPORT(3002, "该采集记录已生成报告"),

    MODEL_NOT_FOUND(4001, "模型不存在"),
    MODEL_DISABLED(4002, "模型已停用"),
    MODEL_PARAM_MISSING(4003, "采集数据缺少模型必需参数"),

    REPORT_NOT_FOUND(5001, "报告不存在"),
    REPORT_GENERATE_FAIL(5002, "报告生成失败"),

    PYTHON_SERVICE_ERROR(6001, "Python模型服务调用失败");

    private final int code;
    private final String message;
}

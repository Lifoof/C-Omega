package com.health.report.framework.interceptor;

import java.lang.reflect.Method;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson2.JSON;
import com.health.report.common.annotation.RepeatSubmit;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.utils.ServletUtils;

/**
 * 防止重复提交拦截器 + 时间戳鉴权（防重放、防爬）
 * 安全版本：未登录接口放行，登录后接口不校验时间戳
 */
@Component
public abstract class RepeatSubmitInterceptor implements HandlerInterceptor
{
    // 6分钟超时（毫秒）
    private static final long TIME_OUT = 6 * 60 * 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        String uri = request.getRequestURI();

        // ===================== 【第一类：完全公开接口】未登录就能访问 =====================
        if (uri.contains("/login")
                || uri.contains("/register")
                || uri.contains("/captchaImage")
                || uri.contains("/smsLogin")
                || uri.contains("/sendSmsCode")
                || uri.contains("/sendEmailCode")
                || uri.contains("/userRegister")
                || uri.contains("/resetPassword")
                || uri.contains("/verifyCode"))
        {
            return true;
        }

        // ===================== 【第二类：登录后可用，但不校验时间戳】 =====================
        // 这些必须登录才能访问，但不需要 timeStop（文件/下载/预览/刷新时间）
        if (uri.contains("/api/getServerTime")      // 登录后刷新时间戳
                || uri.contains("/profile/")         // 文件预览
                || uri.contains("/common/download")  // 下载
                || uri.contains("/common/uploadRoot")// 上传
                || uri.contains("/common/uploadModel")
                || uri.contains("/pdf/"))            // PDF预览
        {
            return true;
        }

        // ===================== 【第三类：所有业务接口】必须校验时间戳 =====================
        String timeStopStr = request.getParameter("timeStop");
        if (timeStopStr == null || timeStopStr.isEmpty()) {
            AjaxResult ajaxResult = AjaxResult.error("鉴权失败：缺少时间戳参数");
            ServletUtils.renderString(response, JSON.toJSONString(ajaxResult));
            return false;
        }

        long timeStop = Long.parseLong(timeStopStr);
        long now = System.currentTimeMillis();
        if (now - timeStop > TIME_OUT) {
            AjaxResult ajaxResult = AjaxResult.error("鉴权失败：请求已超时，请重新登录");
            ServletUtils.renderString(response, JSON.toJSONString(ajaxResult));
            return false;
        }

        // ===================== 原有防重复提交逻辑 不动 =====================
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            RepeatSubmit annotation = method.getAnnotation(RepeatSubmit.class);
            if (annotation != null) {
                if (this.isRepeatSubmit(request, annotation)) {
                    AjaxResult ajaxResult = AjaxResult.error(annotation.message());
                    ServletUtils.renderString(response, JSON.toJSONString(ajaxResult));
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    public abstract boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation);
}
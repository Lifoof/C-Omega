package com.health.report.web.controller.system;

import com.health.report.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TimeController {
    // 获取服务器当前时间戳
    @GetMapping("/getServerTime")
    public long getServerTime() {
        return System.currentTimeMillis();
    }
}

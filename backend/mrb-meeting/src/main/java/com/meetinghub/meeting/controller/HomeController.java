package com.meetinghub.meeting.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页统计控制器
 */
@RestController
@RequestMapping("/meeting/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /** 获取首页统计数据 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.ok(homeService.getStats());
    }
}

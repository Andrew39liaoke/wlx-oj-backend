package com.wlx.ojbackendquestionservice.config;

import com.wlx.ojbackendquestionservice.service.RecommendationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 推荐系统定时任务配置
 */
@Configuration
@EnableScheduling
@Slf4j
public class RecommendationScheduleConfig {

    @Resource
    private RecommendationService recommendationService;

    /**
     * 每天凌晨 2:00 全量计算推荐结果
     * 结果写入 user_recommendation 表
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void computeAllRecommendations() {
        log.info("定时任务触发：开始全量推荐计算");
        recommendationService.computeAllRecommendations();
    }
}

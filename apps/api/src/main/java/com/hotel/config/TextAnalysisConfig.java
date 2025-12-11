package com.hotel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 文本分析配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hotel.text-analysis")
public class TextAnalysisConfig {

    /**
     * 中文停用词列表
     */
    private List<String> stopWords = List.of(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很",
        "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "它", "他", "她"
    );

    /**
     * 评价分类关键词映射
     */
    private Map<String, List<String>> categoryKeywords = Map.of(
        "service", List.of("服务", "服务员", "前台", "接待", "客服", "态度", "热情", "耐心", "专业"),
        "cleanliness", List.of("干净", "清洁", "卫生", "整洁", "灰尘", "垃圾", "脏", "异味"),
        "facilities", List.of("设施", "设备", "房间", "床", "空调", "电视", "热水", "WiFi"),
        "location", List.of("位置", "交通", "方便", "附近", "市中心", "地铁", "公交"),
        "value", List.of("价格", "性价比", "便宜", "贵", "值得", "划算"),
        "breakfast", List.of("早餐", "自助餐", "食物", "品种", "味道", "餐厅")
    );

    /**
     * 积极词汇列表
     */
    private List<String> positiveWords = List.of(
        "好", "棒", "优秀", "满意", "喜欢", "舒适", "干净", "热情", "耐心", "专业"
    );

    /**
     * 消极词汇列表
     */
    private List<String> negativeWords = List.of(
        "差", "糟糕", "失望", "不满", "不舒服", "脏", "乱", "慢", "态度差"
    );

    /**
     * 最小词长
     */
    private int minWordLength = 2;

    /**
     * 最小词频
     */
    private int minWordFrequency = 3;

    /**
     * 是否启用情感分析
     */
    private boolean enableSentimentAnalysis = true;

    /**
     * 最大词云数量
     */
    private int maxWordCloudSize = 100;
}
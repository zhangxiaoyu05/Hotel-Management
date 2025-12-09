package com.hotel.constant;

/**
 * 评分相关常量
 */
public class RatingConstants {

    /**
     * 评分精度缩放因子（保留1位小数）
     */
    public static final double RATING_SCALE = 10.0;

    /**
     * 最小词频阈值
     */
    public static final int MIN_WORD_FREQUENCY = 3;

    /**
     * 默认词云显示数量
     */
    public static final int DEFAULT_WORD_CLOUD_LIMIT = 50;

    /**
     * 最大词云显示数量
     */
    public static final int MAX_WORD_CLOUD_LIMIT = 100;

    /**
     * NPS推荐者阈值
     */
    public static final int NPS_PROMOTER_THRESHOLD = 9;

    /**
     * NPS贬损者阈值
     */
    public static final int NPS_DETRACTOR_THRESHOLD = 6;

    /**
     * 评分范围最小值
     */
    public static final int MIN_RATING = 1;

    /**
     * 评分范围最大值
     */
    public static final int MAX_RATING = 5;

    private RatingConstants() {
        // 工具类，禁止实例化
    }
}
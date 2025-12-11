package com.hotel.service;

import com.hotel.config.TextAnalysisConfig;
import com.hotel.dto.review.statistics.WordCloudDTO;
import com.hotel.dto.review.statistics.SuggestionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文本分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextAnalysisService {

    private final TextAnalysisConfig config;

    /**
     * 生成词云数据
     */
    public List<WordCloudDTO> generateWordCloud(String text, Long hotelId, Integer limit) {
        log.info("开始生成词云数据，酒店ID: {}", hotelId);

        try {
            // 分词和统计
            Map<String, Integer> wordFrequency = extractAndCountWords(text);

            // 计算权重
            int maxFrequency = wordFrequency.values().stream().mapToInt(Integer::intValue).max().orElse(1);

            List<WordCloudDTO> wordCloudList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
                if (entry.getValue() >= 3 && wordCloudList.size() < limit) { // 最小词频为3
                    String word = entry.getKey();
                    int count = entry.getValue();
                    double weight = (double) count / maxFrequency;

                    WordCloudDTO wordCloud = WordCloudDTO.builder()
                        .word(word)
                        .count(count)
                        .weight(weight)
                        .sentiment(analyzeSentiment(word))
                        .category(categorizeWord(word))
                        .build();

                    wordCloudList.add(wordCloud);
                }
            }

            // 按权重排序
            wordCloudList.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));

            log.info("词云生成完成，生成{}个关键词", wordCloudList.size());
            return wordCloudList;

        } catch (Exception e) {
            log.error("生成词云失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成改进建议
     */
    public List<SuggestionDTO> generateSuggestions(Long hotelId, String category) {
        log.info("生成改进建议，酒店ID: {}, 类别: {}", hotelId, category);

        List<SuggestionDTO> suggestions = new ArrayList<>();

        try {
            // 基于类别生成建议
            switch (category.toLowerCase()) {
                case "service":
                    suggestions.addAll(generateServiceSuggestions());
                    break;
                case "cleanliness":
                    suggestions.addAll(generateCleanlinessSuggestions());
                    break;
                case "facilities":
                    suggestions.addAll(generateFacilitiesSuggestions());
                    break;
                case "location":
                    suggestions.addAll(generateLocationSuggestions());
                    break;
                default:
                    suggestions.addAll(generateGeneralSuggestions());
            }

            // 按优先级排序
            suggestions.sort((a, b) -> {
                int priorityOrder = getPriorityOrder(b.getPriority()) - getPriorityOrder(a.getPriority());
                if (priorityOrder != 0) return priorityOrder;
                return Double.compare(b.getExpectedRatingImprovement(), a.getExpectedRatingImprovement());
            });

            log.info("生成改进建议{}条", suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.error("生成改进建议失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 提取并统计词汇
     */
    private Map<String, Integer> extractAndCountWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        // 使用正则表达式提取中文词汇
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]+");
        java.util.regex.Matcher matcher = pattern.matcher(text.toLowerCase());

        Set<String> stopWords = new HashSet<>(config.getStopWords());
        Map<String, Integer> wordCount = new HashMap<>();

        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() >= config.getMinWordLength() && !stopWords.contains(word)) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        // 过滤低频词
        return wordCount.entrySet().stream()
            .filter(entry -> entry.getValue() >= config.getMinWordFrequency())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * 分析词汇情感倾向
     */
    private String analyzeSentiment(String word) {
        Set<String> positiveWords = new HashSet<>(config.getPositiveWords());
        Set<String> negativeWords = new HashSet<>(config.getNegativeWords());

        if (positiveWords.contains(word)) {
            return "positive";
        } else if (negativeWords.contains(word)) {
            return "negative";
        } else {
            return "neutral";
        }
    }

    /**
     * 对词汇进行分类
     */
    private String categorizeWord(String word) {
        Map<String, List<String>> categoryKeywords = config.getCategoryKeywords();
        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            if (entry.getValue().stream().anyMatch(keyword -> word.contains(keyword))) {
                return entry.getKey();
            }
        }
        return "other";
    }

    /**
     * 生成服务改进建议
     */
    private List<SuggestionDTO> generateServiceSuggestions() {
        return Arrays.asList(
            SuggestionDTO.builder()
                .id(1L)
                .category("service")
                .title("提升前台服务响应速度")
                .description("根据客户反馈，前台办理入住和退房手续时间较长，建议增加前台人员配置，优化流程。")
                .priority("high")
                .keywords(new String[]{"前台", "等待", "慢", "效率"})
                .relatedReviewCount(15)
                .expectedRatingImprovement(0.3)
                .difficulty("medium")
                .estimatedCost("5000-10000元/月")
                .implementationTime("1-2个月")
                .analysisResult("客户对前台服务效率的评分比其他维度低15%")
                .createdAt("2025-12-09")
                .build(),

            SuggestionDTO.builder()
                .id(2L)
                .category("service")
                .title("加强员工培训")
                .description("定期开展服务质量培训，提升员工专业技能和服务意识。")
                .priority("medium")
                .keywords(new String[]{"服务", "态度", "专业", "培训"})
                .relatedReviewCount(8)
                .expectedRatingImprovement(0.2)
                .difficulty("low")
                .estimatedCost("3000-5000元/季度")
                .implementationTime("1个月")
                .analysisResult("员工专业能力评分有提升空间")
                .createdAt("2025-12-09")
                .build()
        );
    }

    /**
     * 生成卫生改进建议
     */
    private List<SuggestionDTO> generateCleanlinessSuggestions() {
        return Arrays.asList(
            SuggestionDTO.builder()
                .id(3L)
                .category("cleanliness")
                .title("加强房间清洁标准")
                .description("制定更严格的清洁检查清单，增加清洁频次，特别是高频接触区域。")
                .priority("high")
                .keywords(new String[]{"干净", "清洁", "卫生", "灰尘"})
                .relatedReviewCount(12)
                .expectedRatingImprovement(0.4)
                .difficulty("medium")
                .estimatedCost("8000-15000元/月")
                .implementationTime("1个月")
                .analysisResult("卫生评分低于行业平均水平10%")
                .createdAt("2025-12-09")
                .build()
        );
    }

    /**
     * 生成设施改进建议
     */
    private List<SuggestionDTO> generateFacilitiesSuggestions() {
        return Arrays.asList(
            SuggestionDTO.builder()
                .id(4L)
                .category("facilities")
                .title("升级老旧设备")
                .description("逐步更换老旧的空调、电视等设备，提升客户体验。")
                .priority("medium")
                .keywords(new String[]{"设施", "设备", "旧", "老化"})
                .relatedReviewCount(6)
                .expectedRatingImprovement(0.3)
                .difficulty("high")
                .estimatedCost("50000-100000元")
                .implementationTime("3-6个月")
                .analysisResult("设备老化问题影响客户满意度")
                .createdAt("2025-12-09")
                .build()
        );
    }

    /**
     * 生成位置相关建议
     */
    private List<SuggestionDTO> generateLocationSuggestions() {
        return Arrays.asList(
            SuggestionDTO.builder()
                .id(5L)
                .category("location")
                .title("优化交通指引")
                .description("在网站和预订平台提供更详细的交通指引和地图。")
                .priority("low")
                .keywords(new String[]{"位置", "交通", "指引", "方便"})
                .relatedReviewCount(4)
                .expectedRatingImprovement(0.1)
                .difficulty("low")
                .estimatedCost("1000-2000元")
                .implementationTime("2周")
                .analysisResult("客户对位置信息的完整性有期待")
                .createdAt("2025-12-09")
                .build()
        );
    }

    /**
     * 生成通用改进建议
     */
    private List<SuggestionDTO> generateGeneralSuggestions() {
        return Arrays.asList(
            SuggestionDTO.builder()
                .id(6L)
                .category("general")
                .title("提升整体性价比")
                .description("通过优化运营成本，在保证服务质量的前提下提供更有竞争力的价格。")
                .priority("medium")
                .keywords(new String[]{"价格", "性价比", "值", "优惠"})
                .relatedReviewCount(10)
                .expectedRatingImprovement(0.25)
                .difficulty("high")
                .estimatedCost("需要具体分析")
                .implementationTime("3-6个月")
                .analysisResult("性价比评分有提升空间")
                .createdAt("2025-12-09")
                .build()
        );
    }

    /**
     * 获取优先级权重
     */
    private int getPriorityOrder(String priority) {
        switch (priority.toLowerCase()) {
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 0;
        }
    }
}
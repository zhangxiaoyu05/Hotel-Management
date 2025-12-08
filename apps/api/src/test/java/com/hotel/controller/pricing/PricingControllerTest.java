package com.hotel.controller.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.controller.pricing.PricingController;
import com.hotel.entity.pricing.PricingRule;
import com.hotel.entity.pricing.SpecialPrice;
import com.hotel.entity.pricing.PriceHistory;
import com.hotel.service.PricingService;
import com.hotel.service.pricing.PricingRuleService;
import com.hotel.service.pricing.SpecialPriceService;
import com.hotel.service.pricing.PriceHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PricingController.class)
@DisplayName("价格策略控制器测试")
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PricingService pricingService;

    @MockBean
    private PricingRuleService pricingRuleService;

    @MockBean
    private SpecialPriceService specialPriceService;

    @MockBean
    private PriceHistoryService priceHistoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private PricingRule testRule;
    private SpecialPrice testSpecialPrice;
    private PriceHistory testPriceHistory;

    @BeforeEach
    void setUp() {
        // 设置测试价格规则
        testRule = new PricingRule();
        testRule.setId(1L);
        testRule.setHotelId(1L);
        testRule.setRoomTypeId(1L);
        testRule.setName("周末价格");
        testRule.setRuleType("WEEKEND");
        testRule.setAdjustmentType("PERCENTAGE");
        testRule.setAdjustmentValue(new BigDecimal("20.00"));
        testRule.setIsActive(true);
        testRule.setPriority(100);

        // 设置测试特殊价格
        testSpecialPrice = new SpecialPrice();
        testSpecialPrice.setId(1L);
        testSpecialPrice.setHotelId(1L);
        testSpecialPrice.setRoomTypeId(1L);
        testSpecialPrice.setRoomId(1L);
        testSpecialPrice.setDate(LocalDate.of(2025, 1, 1));
        testSpecialPrice.setPrice(new BigDecimal("299.00"));
        testSpecialPrice.setReason("元旦特惠");

        // 设置测试价格历史
        testPriceHistory = new PriceHistory();
        testPriceHistory.setId(1L);
        testPriceHistory.setHotelId(1L);
        testPriceHistory.setRoomTypeId(1L);
        testPriceHistory.setRoomId(1L);
        testPriceHistory.setOldPrice(new BigDecimal("200.00"));
        testPriceHistory.setNewPrice(new BigDecimal("250.00"));
        testPriceHistory.setChangeType("MANUAL");
        testPriceHistory.setChangeReason("价格调整");
        testPriceHistory.setChangedBy(1L);
        testPriceHistory.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试计算房间价格")
    void testCalculateRoomPrice() throws Exception {
        // Given
        when(pricingService.calculateRoomPrice(anyLong(), any(LocalDate.class)))
                .thenReturn(new BigDecimal("240.00"));

        // When & Then
        mockMvc.perform(get("/v1/pricing/calculate/room/1")
                        .param("date", "2025-01-18")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(240.00))
                .andExpect(jsonPath("$.message").value("价格计算成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试计算房间类型价格")
    void testCalculateRoomTypePrice() throws Exception {
        // Given
        when(pricingService.calculateRoomTypePrice(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(new BigDecimal("240.00"));

        // When & Then
        mockMvc.perform(get("/v1/pricing/calculate/room-type/1")
                        .param("hotelId", "1")
                        .param("date", "2025-01-18")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(240.00))
                .andExpect(jsonPath("$.message").value("价格计算成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试批量计算房间价格")
    void testCalculateRoomPricesBatch() throws Exception {
        // Given
        Map<LocalDate, BigDecimal> priceMap = new HashMap<>();
        priceMap.put(LocalDate.of(2025, 1, 17), new BigDecimal("200.00"));
        priceMap.put(LocalDate.of(2025, 1, 18), new BigDecimal("240.00"));
        priceMap.put(LocalDate.of(2025, 1, 19), new BigDecimal("240.00"));

        when(pricingService.calculateRoomPricesForDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(priceMap);

        // When & Then
        mockMvc.perform(get("/v1/pricing/calculate/room/1/batch")
                        .param("startDate", "2025-01-17")
                        .param("endDate", "2025-01-19")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data['2025-01-17']").value(200.00))
                .andExpect(jsonPath("$.data['2025-01-18']").value(240.00))
                .andExpect(jsonPath("$.data['2025-01-19']").value(240.00))
                .andExpect(jsonPath("$.message").value("批量价格计算成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试创建价格规则")
    void testCreatePricingRule() throws Exception {
        // Given
        when(pricingRuleService.createRule(any(PricingRule.class)))
                .thenReturn(testRule);

        String requestBody = objectMapper.writeValueAsString(testRule);

        // When & Then
        mockMvc.perform(post("/v1/pricing/rules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("周末价格"))
                .andExpect(jsonPath("$.message").value("价格规则创建成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试更新价格规则")
    void testUpdatePricingRule() throws Exception {
        // Given
        when(pricingRuleService.updateRule(eq(1L), any(PricingRule.class)))
                .thenReturn(testRule);

        String requestBody = objectMapper.writeValueAsString(testRule);

        // When & Then
        mockMvc.perform(put("/v1/pricing/rules/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("周末价格"))
                .andExpect(jsonPath("$.message").value("价格规则更新成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试删除价格规则")
    void testDeletePricingRule() throws Exception {
        // When & Then
        mockMvc.perform(delete("/v1/pricing/rules/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("价格规则删除成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试切换规则状态")
    void testToggleRuleStatus() throws Exception {
        // When & Then
        mockMvc.perform(patch("/v1/pricing/rules/1/status")
                        .with(csrf())
                        .param("active", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("价格规则已激活"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试获取价格规则详情")
    void testGetPricingRule() throws Exception {
        // Given
        when(pricingRuleService.getRuleById(1L))
                .thenReturn(testRule);

        // When & Then
        mockMvc.perform(get("/v1/pricing/rules/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("周末价格"))
                .andExpect(jsonPath("$.message").value("获取价格规则成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试获取价格规则列表")
    void testGetPricingRules() throws Exception {
        // Given
        List<PricingRule> rules = Arrays.asList(testRule);
        when(pricingRuleService.getRulesByHotelId(eq(1L), anyBoolean()))
                .thenReturn(rules);

        // When & Then
        mockMvc.perform(get("/v1/pricing/rules")
                        .param("hotelId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("周末价格"))
                .andExpect(jsonPath("$.message").value("获取价格规则列表成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试批量删除价格规则")
    void testBatchDeletePricingRules() throws Exception {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        String requestBody = objectMapper.writeValueAsString(ids);

        // When & Then
        mockMvc.perform(delete("/v1/pricing/rules/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("批量删除价格规则成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试创建特殊价格")
    void testCreateSpecialPrice() throws Exception {
        // Given
        when(specialPriceService.createSpecialPrice(any(SpecialPrice.class), anyLong()))
                .thenReturn(testSpecialPrice);

        String requestBody = objectMapper.writeValueAsString(testSpecialPrice);

        // When & Then
        mockMvc.perform(post("/v1/pricing/special-prices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.price").value(299.00))
                .andExpect(jsonPath("$.message").value("特殊价格创建成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试获取特殊价格列表")
    void testGetSpecialPrices() throws Exception {
        // Given
        List<SpecialPrice> specialPrices = Arrays.asList(testSpecialPrice);
        when(specialPriceService.getSpecialPricesByHotelId(eq(1L), any(), any()))
                .thenReturn(specialPrices);

        // When & Then
        mockMvc.perform(get("/v1/pricing/special-prices")
                        .param("hotelId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].price").value(299.00))
                .andExpect(jsonPath("$.message").value("获取特殊价格列表成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试获取价格历史记录")
    void testGetPriceHistory() throws Exception {
        // Given
        List<PriceHistory> historyList = Arrays.asList(testPriceHistory);
        when(priceHistoryService.getPriceHistoryByHotelId(eq(1L), anyInt()))
                .thenReturn(historyList);

        // When & Then
        mockMvc.perform(get("/v1/pricing/history")
                        .param("hotelId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].oldPrice").value(200.00))
                .andExpect(jsonPath("$.data[0].newPrice").value(250.00))
                .andExpect(jsonPath("$.message").value("获取价格历史记录成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试获取价格变更统计")
    void testGetPriceChangeStatistics() throws Exception {
        // Given
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalChanges", 10);
        statistics.put("increaseCount", 6);
        statistics.put("decreaseCount", 4);
        statistics.put("period", "30天");

        when(priceHistoryService.getPriceChangeStatistics(eq(1L), anyInt()))
                .thenReturn(statistics);

        // When & Then
        mockMvc.perform(get("/v1/pricing/history/statistics")
                        .param("hotelId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalChanges").value(10))
                .andExpect(jsonPath("$.data.increaseCount").value(6))
                .andExpect(jsonPath("$.data.decreaseCount").value(4))
                .andExpect(jsonPath("$.message").value("获取价格变更统计成功"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("测试获取节假日信息")
    void testGetHolidays() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/pricing/holidays")
                        .param("year", "2025")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取节假日信息成功"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("测试检查是否为节假日")
    void testCheckHoliday() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/pricing/holidays/check")
                        .param("date", "2025-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value("2025-01-01"))
                .andExpect(jsonPath("$.message").value("节假日检查完成"));
    }

    @Test
    @DisplayName("测试未授权访问")
    void testUnauthorizedAccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/v1/pricing/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRule)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("测试权限不足")
    void testInsufficientPermissions() throws Exception {
        // When & Then
        mockMvc.perform(post("/v1/pricing/rules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRule)))
                .andExpect(status().isForbidden());
    }
}
package com.hotel.service.pricing;

import com.hotel.entity.pricing.PricingRule;
import com.hotel.enums.PricingRuleType;
import com.hotel.enums.AdjustmentType;
import com.hotel.repository.pricing.PricingRuleRepository;
import com.hotel.exception.BusinessException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("价格规则服务测试")
class PricingRuleServiceTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private PricingRuleService pricingRuleService;

    private PricingRule testRule;
    private PricingRule createRuleRequest;

    @BeforeEach
    void setUp() {
        // 设置测试规则
        testRule = new PricingRule();
        testRule.setId(1L);
        testRule.setHotelId(1L);
        testRule.setRoomTypeId(1L);
        testRule.setName("周末价格上浮");
        testRule.setRuleType("WEEKEND");
        testRule.setAdjustmentType("PERCENTAGE");
        testRule.setAdjustmentValue(new BigDecimal("20.00"));
        testRule.setDaysOfWeekList(Arrays.asList(6, 7));
        testRule.setStartDate(LocalDate.of(2025, 1, 1));
        testRule.setEndDate(LocalDate.of(2025, 12, 31));
        testRule.setIsActive(true);
        testRule.setPriority(100);

        // 设置创建请求
        createRuleRequest = new PricingRule();
        createRuleRequest.setHotelId(1L);
        createRuleRequest.setRoomTypeId(1L);
        createRuleRequest.setName("新规则");
        createRuleRequest.setRuleType("WEEKEND");
        createRuleRequest.setAdjustmentType("PERCENTAGE");
        createRuleRequest.setAdjustmentValue(new BigDecimal("15.00"));
        createRuleRequest.setDaysOfWeekList(Arrays.asList(6, 7));
        createRuleRequest.setIsActive(true);
        createRuleRequest.setPriority(80);
    }

    @Test
    @DisplayName("测试创建价格规则成功")
    void testCreateRule_Success() {
        // Given
        when(pricingRuleRepository.countByName(anyLong(), anyString(), isNull())).thenReturn(0);
        when(pricingRuleRepository.insert(any(PricingRule.class))).thenReturn(1);

        // When
        PricingRule result = pricingRuleService.createRule(createRuleRequest);

        // Then
        assertNotNull(result);
        assertEquals("新规则", result.getName());
        assertEquals("WEEKEND", result.getRuleType());
        assertEquals("PERCENTAGE", result.getAdjustmentType());
        assertEquals(new BigDecimal("15.00"), result.getAdjustmentValue());
        assertTrue(result.getIsActive());
        verify(pricingRuleRepository).insert(any(PricingRule.class));
    }

    @Test
    @DisplayName("测试创建价格规则 - 名称重复")
    void testCreateRule_NameDuplicate() {
        // Given
        when(pricingRuleRepository.countByName(anyLong(), anyString(), isNull())).thenReturn(1);

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pricingRuleService.createRule(createRuleRequest)
        );

        assertEquals("规则名称 '新规则' 已存在", exception.getMessage());
        verify(pricingRuleRepository, never()).insert(any(PricingRule.class));
    }

    @Test
    @DisplayName("测试创建价格规则 - 验证失败")
    void testCreateRule_ValidationFailure() {
        // Given
        PricingRule invalidRule = new PricingRule();
        invalidRule.setHotelId(null); // 酒店ID为空

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pricingRuleService.createRule(invalidRule)
        );

        assertEquals("酒店ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("测试更新价格规则成功")
    void testUpdateRule_Success() {
        // Given
        PricingRule updateRequest = new PricingRule();
        updateRequest.setName("更新后的规则");
        updateRequest.setRuleType("HOLIDAY");
        updateRequest.setAdjustmentType("FIXED_AMOUNT");
        updateRequest.setAdjustmentValue(new BigDecimal("50.00"));
        updateRequest.setIsActive(false);
        updateRequest.setPriority(150);

        when(pricingRuleRepository.selectById(1L)).thenReturn(testRule);
        when(pricingRuleRepository.countByName(anyLong(), anyString(), eq(1L))).thenReturn(0);
        when(pricingRuleRepository.updateById(any(PricingRule.class))).thenReturn(1);

        // When
        PricingRule result = pricingRuleService.updateRule(1L, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("更新后的规则", result.getName());
        assertEquals("HOLIDAY", result.getRuleType());
        assertEquals("FIXED_AMOUNT", result.getAdjustmentType());
        assertEquals(new BigDecimal("50.00"), result.getAdjustmentValue());
        assertFalse(result.getIsActive());
        assertEquals(150, result.getPriority());
        verify(pricingRuleRepository).updateById(any(PricingRule.class));
    }

    @Test
    @DisplayName("测试更新价格规则 - 规则不存在")
    void testUpdateRule_RuleNotFound() {
        // Given
        when(pricingRuleRepository.selectById(1L)).thenReturn(null);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> pricingRuleService.updateRule(1L, createRuleRequest)
        );

        assertEquals("价格规则不存在: 1", exception.getMessage());
        verify(pricingRuleRepository, never()).updateById(any(PricingRule.class));
    }

    @Test
    @DisplayName("测试删除价格规则成功")
    void testDeleteRule_Success() {
        // Given
        when(pricingRuleRepository.selectById(1L)).thenReturn(testRule);
        when(pricingRuleRepository.deleteById(1L)).thenReturn(1);

        // When
        pricingRuleService.deleteRule(1L);

        // Then
        verify(pricingRuleRepository).deleteById(1L);
    }

    @Test
    @DisplayName("测试删除价格规则 - 规则不存在")
    void testDeleteRule_RuleNotFound() {
        // Given
        when(pricingRuleRepository.selectById(1L)).thenReturn(null);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> pricingRuleService.deleteRule(1L)
        );

        assertEquals("价格规则不存在: 1", exception.getMessage());
        verify(pricingRuleRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("测试切换规则状态 - 激活")
    void testToggleRuleStatus_Activate() {
        // Given
        when(pricingRuleRepository.selectById(1L)).thenReturn(testRule);
        when(pricingRuleRepository.updateById(any(PricingRule.class))).thenReturn(1);

        // When
        pricingRuleService.toggleRuleStatus(1L, true);

        // Then
        assertTrue(testRule.getIsActive());
        verify(pricingRuleRepository).updateById(testRule);
    }

    @Test
    @DisplayName("测试切换规则状态 - 停用")
    void testToggleRuleStatus_Deactivate() {
        // Given
        when(pricingRuleRepository.selectById(1L)).thenReturn(testRule);
        when(pricingRuleRepository.updateById(any(PricingRule.class))).thenReturn(1);

        // When
        pricingRuleService.toggleRuleStatus(1L, false);

        // Then
        assertFalse(testRule.getIsActive());
        verify(pricingRuleRepository).updateById(testRule);
    }

    @Test
    @DisplayName("测试获取价格规则详情")
    void testGetRuleById_Success() {
        // Given
        when(pricingRuleRepository.selectById(1L)).thenReturn(testRule);

        // When
        PricingRule result = pricingRuleService.getRuleById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("周末价格上浮", result.getName());
    }

    @Test
    @DisplayName("测试获取价格规则详情 - 规则不存在")
    void testGetRuleById_RuleNotFound() {
        // Given
        when(pricingRuleRepository.selectById(1L)).thenReturn(null);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> pricingRuleService.getRuleById(1L)
        );

        assertEquals("价格规则不存在: 1", exception.getMessage());
    }

    @Test
    @DisplayName("测试获取酒店的价格规则列表")
    void testGetRulesByHotelId_Success() {
        // Given
        List<PricingRule> expectedRules = Arrays.asList(testRule);
        when(pricingRuleRepository.findByHotelId(1L)).thenReturn(expectedRules);

        // When
        List<PricingRule> result = pricingRuleService.getRulesByHotelId(1L, false);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRule, result.get(0));
    }

    @Test
    @DisplayName("测试获取活跃价格规则列表")
    void testGetRulesByHotelId_ActiveOnly() {
        // Given
        List<PricingRule> activeRules = Arrays.asList(testRule);
        when(pricingRuleRepository.findByHotelId(1L)).thenReturn(activeRules);

        // When
        List<PricingRule> result = pricingRuleService.getRulesByHotelId(1L, true);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    @DisplayName("测试批量删除价格规则")
    void testBatchDeleteRules_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(pricingRuleRepository.selectById(anyLong())).thenReturn(testRule);
        when(pricingRuleRepository.deleteById(anyLong())).thenReturn(1);

        // When
        pricingRuleService.batchDeleteRules(ids);

        // Then
        verify(pricingRuleRepository, times(3)).deleteById(anyLong());
    }

    @Test
    @DisplayName("测试批量激活价格规则")
    void testBatchToggleRules_Activate() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L);
        when(pricingRuleRepository.selectById(anyLong())).thenReturn(testRule);
        when(pricingRuleRepository.updateById(any(PricingRule.class))).thenReturn(1);

        // When
        pricingRuleService.batchToggleRules(ids, true);

        // Then
        verify(pricingRuleRepository, times(2)).updateById(any(PricingRule.class));
        assertTrue(testRule.getIsActive());
    }

    @Test
    @DisplayName("测试创建周末规则")
    void testCreateWeekendRule_Success() {
        // Given
        when(pricingRuleRepository.countByName(anyLong(), anyString(), isNull())).thenReturn(0);
        when(pricingRuleRepository.insert(any(PricingRule.class))).thenReturn(1);

        // When
        PricingRule result = pricingRuleService.createWeekendRule(1L, 1L, new BigDecimal("25.00"));

        // Then
        assertNotNull(result);
        assertEquals("周末价格调整", result.getName());
        assertEquals("WEEKEND", result.getRuleType());
        assertEquals("PERCENTAGE", result.getAdjustmentType());
        assertEquals(new BigDecimal("25.00"), result.getAdjustmentValue());
        assertEquals(Arrays.asList(6, 7), result.getDaysOfWeekList());
        assertEquals(100, result.getPriority());
    }

    @Test
    @DisplayName("测试创建节假日规则")
    void testCreateHolidayRule_Success() {
        // Given
        when(pricingRuleRepository.countByName(anyLong(), anyString(), isNull())).thenReturn(0);
        when(pricingRuleRepository.insert(any(PricingRule.class))).thenReturn(1);

        // When
        PricingRule result = pricingRuleService.createHolidayRule(1L, null, new BigDecimal("35.00"));

        // Then
        assertNotNull(result);
        assertEquals("节假日价格调整", result.getName());
        assertEquals("HOLIDAY", result.getRuleType());
        assertEquals("PERCENTAGE", result.getAdjustmentType());
        assertEquals(new BigDecimal("35.00"), result.getAdjustmentValue());
        assertEquals(200, result.getPriority());
    }

    @Test
    @DisplayName("测试验证规则 - 调整值超出范围")
    void testValidateRule_AdjustmentValueOutOfRange() {
        // Given
        PricingRule invalidRule = new PricingRule();
        invalidRule.setHotelId(1L);
        invalidRule.setName("无效规则");
        invalidRule.setRuleType("PERCENTAGE");
        invalidRule.setAdjustmentType("PERCENTAGE");
        invalidRule.setAdjustmentValue(new BigDecimal("1500.00")); // 超过1000%

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pricingRuleService.createRule(invalidRule)
        );

        assertEquals("百分比调整不能超过1000%", exception.getMessage());
    }

    @Test
    @DisplayName("测试验证规则 - 日期范围无效")
    void testValidateRule_InvalidDateRange() {
        // Given
        PricingRule invalidRule = new PricingRule();
        invalidRule.setHotelId(1L);
        invalidRule.setName("无效规则");
        invalidRule.setRuleType("CUSTOM");
        invalidRule.setAdjustmentType("FIXED_AMOUNT");
        invalidRule.setAdjustmentValue(new BigDecimal("50.00"));
        invalidRule.setStartDate(LocalDate.of(2025, 1, 31));
        invalidRule.setEndDate(LocalDate.of(2025, 1, 1)); // 开始日期晚于结束日期

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pricingRuleService.createRule(invalidRule)
        );

        assertEquals("开始日期不能晚于结束日期", exception.getMessage());
    }
}
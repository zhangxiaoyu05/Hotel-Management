package com.hotel.service.review.incentive;

import com.hotel.entity.Order;
import com.hotel.entity.User;
import com.hotel.repository.OrderRepository;
import com.hotel.repository.UserRepository;
import com.hotel.service.ReviewReminderService;
import com.hotel.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewReminderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReviewReminderService reviewReminderService;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(100L);
        testOrder.setHotelId(1L);
        testOrder.setRoomId(1L);
        testOrder.setCheckOutDate(LocalDateTime.now().minusDays(2));
        testOrder.setStatus("COMPLETED");
    }

    @Test
    void testSendReminders_24HoursAfterCheckout() {
        // Arrange
        List<Order> ordersNeedingReminders = Arrays.asList(testOrder);
        when(orderRepository.findOrdersNeedingReviewReminder(any(LocalDateTime.class)))
            .thenReturn(ordersNeedingReminders);
        when(userRepository.selectById(100L)).thenReturn(testUser);

        // Act
        reviewReminderService.sendReminders();

        // Assert
        verify(emailService).sendReviewReminderEmail(eq(testUser.getEmail()), any());
    }

    @Test
    void testSendReminders_NoOrdersNeedingReminder() {
        // Arrange
        when(orderRepository.findOrdersNeedingReviewReminder(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // Act
        reviewReminderService.sendReminders();

        // Assert
        verify(emailService, never()).sendReviewReminderEmail(any(), any());
    }

    @Test
    void testSendReminders_UserNotFound() {
        // Arrange
        List<Order> ordersNeedingReminders = Arrays.asList(testOrder);
        when(orderRepository.findOrdersNeedingReviewReminder(any(LocalDateTime.class)))
            .thenReturn(ordersNeedingReminders);
        when(userRepository.selectById(100L)).thenReturn(null);

        // Act
        reviewReminderService.sendReminders();

        // Assert
        verify(emailService, never()).sendReviewReminderEmail(any(), any());
    }

    @Test
    void testSendReminders_EmailFailure() {
        // Arrange
        List<Order> ordersNeedingReminders = Arrays.asList(testOrder);
        when(orderRepository.findOrdersNeedingReviewReminder(any(LocalDateTime.class)))
            .thenReturn(ordersNeedingReminders);
        when(userRepository.selectById(100L)).thenReturn(testUser);
        doThrow(new RuntimeException("邮件发送失败"))
            .when(emailService).sendReviewReminderEmail(any(), any());

        // Act
        reviewReminderService.sendReminders();

        // Assert
        verify(emailService).sendReviewReminderEmail(eq(testUser.getEmail()), any());
    }

    @Test
    void testShouldSendReminder_FirstReminder() {
        // 使用反射调用私有方法进行测试
        try {
            var method = ReviewReminderService.class.getDeclaredMethod(
                "shouldSendReminder", Order.class);
            method.setAccessible(true);

            // 测试需要提醒的情况（退房后2天）
            testOrder.setCheckOutDate(LocalDateTime.now().minusDays(2));
            boolean shouldSend = (boolean) method.invoke(reviewReminderService, testOrder);
            assertTrue(shouldSend, "退房后2天应该发送首次提醒");

            // 测试不需要提醒的情况（刚退房）
            testOrder.setCheckOutDate(LocalDateTime.now().minusHours(12));
            shouldSend = (boolean) method.invoke(reviewReminderService, testOrder);
            assertFalse(shouldSend, "刚退房不应该发送提醒");

        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }
}
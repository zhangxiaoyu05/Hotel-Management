package com.hotel.service;

import com.hotel.dto.hotel.*;
import com.hotel.entity.Hotel;
import com.hotel.enums.HotelStatus;
import com.hotel.repository.HotelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private HotelService hotelService;

    private Hotel testHotel;
    private CreateHotelRequest createHotelRequest;
    private UpdateHotelRequest updateHotelRequest;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("测试酒店");
        testHotel.setAddress("测试地址");
        testHotel.setPhone("13800138000");
        testHotel.setDescription("测试描述");
        testHotel.setFacilities("[\"WiFi\", \"停车场\"]");
        testHotel.setImages("[\"image1.jpg\", \"image2.jpg\"]");
        testHotel.setStatus(HotelStatus.ACTIVE.name());
        testHotel.setCreatedBy(1L);
        testHotel.setCreatedAt(LocalDateTime.now());
        testHotel.setUpdatedAt(LocalDateTime.now());
        testHotel.setDeleted(0);

        createHotelRequest = new CreateHotelRequest();
        createHotelRequest.setName("新酒店");
        createHotelRequest.setAddress("新地址");
        createHotelRequest.setPhone("13900139000");
        createHotelRequest.setDescription("新描述");
        createHotelRequest.setFacilities(Arrays.asList("WiFi", "游泳池"));
        createHotelRequest.setImages(Arrays.asList("image1.jpg"));

        updateHotelRequest = new UpdateHotelRequest();
        updateHotelRequest.setName("更新酒店");
        updateHotelRequest.setAddress("更新地址");
    }

    @Test
    void testCreateHotel_Success() throws JsonProcessingException {
        // Given
        when(hotelRepository.selectByName(anyString())).thenReturn(null);
        when(hotelRepository.insert(any(Hotel.class))).thenReturn(1);
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"WiFi\", \"游泳池\"]");
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(Arrays.asList("WiFi", "游泳池"));

        // When
        HotelResponse response = hotelService.createHotel(createHotelRequest, 1L);

        // Then
        assertNotNull(response);
        verify(hotelRepository).insert(any(Hotel.class));
    }

    @Test
    void testCreateHotel_NameAlreadyExists() {
        // Given
        when(hotelRepository.selectByName(anyString())).thenReturn(testHotel);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.createHotel(createHotelRequest, 1L));
        assertEquals("酒店名称已存在", exception.getMessage());
    }

    @Test
    void testGetHotelById_Success() {
        // Given
        when(hotelRepository.selectById(1L)).thenReturn(testHotel);
        when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenReturn(Arrays.asList("WiFi", "停车场"))
                .thenReturn(Arrays.asList("image1.jpg", "image2.jpg"));

        // When
        HotelResponse response = hotelService.getHotelById(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("测试酒店", response.getName());
        assertEquals("测试地址", response.getAddress());
    }

    @Test
    void testGetHotelById_NotFound() {
        // Given
        when(hotelRepository.selectById(1L)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.getHotelById(1L));
        assertEquals("酒店不存在", exception.getMessage());
    }

    @Test
    void testGetHotels() {
        // Given
        Page<Hotel> page = new Page<>(0, 20);
        page.setRecords(Arrays.asList(testHotel));
        page.setTotal(1);
        page.setPages(1);

        when(hotelRepository.selectHotelsWithPage(any(Page.class), anyString(),
                anyString(), anyString(), anyString())).thenReturn(page);
        when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenReturn(Arrays.asList("WiFi", "停车场"))
                .thenReturn(Arrays.asList("image1.jpg", "image2.jpg"));

        // When
        HotelListResponse response = hotelService.getHotels(0, 20, null, null, "createdAt", "DESC");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void testUpdateHotel_Success() throws JsonProcessingException {
        // Given
        when(hotelRepository.selectById(1L)).thenReturn(testHotel);
        when(hotelRepository.selectByName("更新酒店")).thenReturn(null);
        when(hotelRepository.updateById(any(Hotel.class))).thenReturn(1);
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"WiFi\", \"游泳池\"]");
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(Arrays.asList("WiFi", "游泳池"));

        // When
        HotelResponse response = hotelService.updateHotel(1L, updateHotelRequest);

        // Then
        assertNotNull(response);
        verify(hotelRepository).updateById(any(Hotel.class));
    }

    @Test
    void testDeleteHotel_Success() {
        // Given
        when(hotelRepository.selectById(1L)).thenReturn(testHotel);
        when(hotelRepository.updateById(any(Hotel.class))).thenReturn(1);

        // When
        hotelService.deleteHotel(1L);

        // Then
        verify(hotelRepository).updateById(any(Hotel.class));
    }

    @Test
    void testUpdateHotelStatus_Success() {
        // Given
        UpdateHotelStatusRequest statusRequest = new UpdateHotelStatusRequest();
        statusRequest.setStatus(HotelStatus.INACTIVE.name());

        when(hotelRepository.selectById(1L)).thenReturn(testHotel);
        when(hotelRepository.updateById(any(Hotel.class))).thenReturn(1);

        // When
        HotelResponse response = hotelService.updateHotelStatus(1L, statusRequest);

        // Then
        assertNotNull(response);
        verify(hotelRepository).updateById(any(Hotel.class));
    }
}
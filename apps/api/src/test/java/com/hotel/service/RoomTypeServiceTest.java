package com.hotel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.roomtype.*;
import com.hotel.entity.RoomType;
import com.hotel.enums.RoomTypeStatus;
import com.hotel.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("房间类型服务测试")
class RoomTypeServiceTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private HotelService hotelService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RoomTypeService roomTypeService;

    private RoomType mockRoomType;
    private CreateRoomTypeRequest createRequest;
    private UpdateRoomTypeRequest updateRequest;
    private HotelResponse mockHotelResponse;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Mock Hotel Response
        mockHotelResponse = new HotelResponse();
        mockHotelResponse.setId(1L);
        mockHotelResponse.setName("测试酒店");

        // Mock RoomType Entity
        mockRoomType = new RoomType();
        mockRoomType.setId(1L);
        mockRoomType.setHotelId(1L);
        mockRoomType.setName("标准间");
        mockRoomType.setCapacity(2);
        mockRoomType.setBasePrice(new BigDecimal("299.00"));
        mockRoomType.setFacilities("[\"WiFi\", \"空调\", \"电视\"]");
        mockRoomType.setDescription("舒适的标准间");
        mockRoomType.setIconUrl("https://example.com/icon.png");
        mockRoomType.setStatus(RoomTypeStatus.ACTIVE.name());
        mockRoomType.setCreatedAt(LocalDateTime.now());
        mockRoomType.setUpdatedAt(LocalDateTime.now());

        // Create Request
        createRequest = new CreateRoomTypeRequest();
        createRequest.setHotelId(1L);
        createRequest.setName("豪华套房");
        createRequest.setCapacity(4);
        createRequest.setBasePrice(new BigDecimal("888.00"));
        createRequest.setFacilities(Arrays.asList("WiFi", "空调", "电视", "迷你吧"));
        createRequest.setDescription("豪华的套房");
        createRequest.setIconUrl("https://example.com/luxury-icon.png");

        // Update Request
        updateRequest = new UpdateRoomTypeRequest();
        updateRequest.setName("商务间");
        updateRequest.setCapacity(2);
        updateRequest.setBasePrice(new BigDecimal("399.00"));
        updateRequest.setFacilities(Arrays.asList("WiFi", "空调", "办公桌"));
        updateRequest.setDescription("适合商务人士的房间");
        updateRequest.setIconUrl("https://example.com/business-icon.png");
        updateRequest.setStatus(RoomTypeStatus.ACTIVE);

        // Mock JSON operations
        when(objectMapper.writeValueAsString(any(List.class))).thenReturn("[\"WiFi\", \"空调\"]");
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Arrays.asList("WiFi", "空调"));
    }

    @Test
    @DisplayName("创建房间类型 - 成功")
    void createRoomType_Success() throws Exception {
        // Arrange
        when(hotelService.getHotelById(1L)).thenReturn(mockHotelResponse);
        when(roomTypeRepository.selectByHotelIdAndName(1L, "豪华套房")).thenReturn(null);
        when(roomTypeRepository.insert(any(RoomType.class))).thenReturn(1);
        when(roomTypeRepository.selectById(anyLong())).thenReturn(mockRoomType);

        // Act
        RoomTypeResponse result = roomTypeService.createRoomType(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(hotelService, times(1)).getHotelById(1L);
        verify(roomTypeRepository, times(1)).selectByHotelIdAndName(1L, "豪华套房");
        verify(roomTypeRepository, times(1)).insert(any(RoomType.class));
    }

    @Test
    @DisplayName("创建房间类型 - 酒店不存在")
    void createRoomType_HotelNotFound() {
        // Arrange
        when(hotelService.getHotelById(1L)).thenThrow(new RuntimeException("酒店不存在"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> roomTypeService.createRoomType(createRequest));
        verify(hotelService, times(1)).getHotelById(1L);
        verify(roomTypeRepository, never()).insert(any());
    }

    @Test
    @DisplayName("创建房间类型 - 名称已存在")
    void createRoomType_NameAlreadyExists() {
        // Arrange
        when(hotelService.getHotelById(1L)).thenReturn(mockHotelResponse);
        when(roomTypeRepository.selectByHotelIdAndName(1L, "豪华套房"))
                .thenReturn(mockRoomType);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(createRequest));
        assertEquals("该酒店下房间类型名称已存在", exception.getMessage());
        verify(roomTypeRepository, never()).insert(any());
    }

    @Test
    @DisplayName("更新房间类型 - 成功")
    void updateRoomType_Success() throws Exception {
        // Arrange
        when(roomTypeRepository.selectById(1L)).thenReturn(mockRoomType);
        when(roomTypeRepository.selectByHotelIdAndName(1L, "商务间")).thenReturn(null);
        when(roomTypeRepository.updateById(any(RoomType.class))).thenReturn(1);

        // Act
        RoomTypeResponse result = roomTypeService.updateRoomType(1L, updateRequest);

        // Assert
        assertNotNull(result);
        verify(roomTypeRepository, times(1)).selectById(1L);
        verify(roomTypeRepository, times(1)).updateById(any(RoomType.class));
    }

    @Test
    @DisplayName("更新房间类型 - 房间类型不存在")
    void updateRoomType_RoomTypeNotFound() {
        // Arrange
        when(roomTypeRepository.selectById(1L)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> roomTypeService.updateRoomType(1L, updateRequest));
        verify(roomTypeRepository, times(1)).selectById(1L);
        verify(roomTypeRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("更新房间类型 - 名称重复")
    void updateRoomType_NameDuplicate() {
        // Arrange
        RoomType existingRoomType = new RoomType();
        existingRoomType.setId(2L);
        existingRoomType.setHotelId(1L);
        existingRoomType.setName("商务间");

        when(roomTypeRepository.selectById(1L)).thenReturn(mockRoomType);
        when(roomTypeRepository.selectByHotelIdAndName(1L, "商务间")).thenReturn(existingRoomType);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> roomTypeService.updateRoomType(1L, updateRequest));
        verify(roomTypeRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("删除房间类型 - 成功")
    void deleteRoomType_Success() {
        // Arrange
        when(roomTypeRepository.selectById(1L)).thenReturn(mockRoomType);
        when(roomTypeRepository.countAssociatedRooms(1L)).thenReturn(0);
        when(roomTypeRepository.deleteById(1L)).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> roomTypeService.deleteRoomType(1L));

        // Assert
        verify(roomTypeRepository, times(1)).selectById(1L);
        verify(roomTypeRepository, times(1)).countAssociatedRooms(1L);
        verify(roomTypeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("删除房间类型 - 房间类型不存在")
    void deleteRoomType_RoomTypeNotFound() {
        // Arrange
        when(roomTypeRepository.selectById(1L)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> roomTypeService.deleteRoomType(1L));
        verify(roomTypeRepository, times(1)).selectById(1L);
        verify(roomTypeRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除房间类型 - 有关联房间")
    void deleteRoomType_HasAssociatedRooms() {
        // Arrange
        when(roomTypeRepository.selectById(1L)).thenReturn(mockRoomType);
        when(roomTypeRepository.countAssociatedRooms(1L)).thenReturn(5);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomTypeService.deleteRoomType(1L));
        assertEquals("该房间类型下还有房间，无法删除", exception.getMessage());
        verify(roomTypeRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("根据ID获取房间类型 - 成功")
    void getRoomTypeById_Success() throws Exception {
        // Arrange
        when(roomTypeRepository.selectById(1L)).thenReturn(mockRoomType);
        when(hotelService.getHotelById(1L)).thenReturn(mockHotelResponse);

        // Act
        RoomTypeResponse result = roomTypeService.getRoomTypeById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("标准间", result.getName());
        verify(roomTypeRepository, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("根据ID获取房间类型 - 不存在")
    void getRoomTypeById_NotFound() {
        // Arrange
        when(roomTypeRepository.selectById(1L)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> roomTypeService.getRoomTypeById(1L));
        verify(roomTypeRepository, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("根据酒店ID获取房间类型列表 - 成功")
    void getRoomTypesByHotelId_Success() throws Exception {
        // Arrange
        List<RoomType> roomTypes = Arrays.asList(mockRoomType);
        when(roomTypeRepository.selectByHotelId(1L)).thenReturn(roomTypes);
        when(hotelService.getHotelById(1L)).thenReturn(mockHotelResponse);

        // Act
        List<RoomTypeResponse> result = roomTypeService.getRoomTypesByHotelId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(hotelService, times(1)).getHotelById(1L);
        verify(roomTypeRepository, times(1)).selectByHotelId(1L);
    }

    @Test
    @DisplayName("根据状态获取房间类型列表 - 成功")
    void getRoomTypesByStatus_Success() throws Exception {
        // Arrange
        List<RoomType> roomTypes = Arrays.asList(mockRoomType);
        when(roomTypeRepository.selectByStatus("ACTIVE")).thenReturn(roomTypes);

        // Act
        List<RoomTypeResponse> result = roomTypeService.getRoomTypesByStatus("ACTIVE");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
        verify(roomTypeRepository, times(1)).selectByStatus("ACTIVE");
    }

    @Test
    @DisplayName("更新房间类型状态 - 成功")
    void updateRoomTypeStatus_Success() throws Exception {
        // Arrange
        when(roomTypeRepository.selectById(1L)).thenReturn(mockRoomType);
        when(roomTypeRepository.updateById(any(RoomType.class))).thenReturn(1);

        // Act
        RoomTypeResponse result = roomTypeService.updateRoomTypeStatus(1L, RoomTypeStatus.INACTIVE);

        // Assert
        assertNotNull(result);
        verify(roomTypeRepository, times(1)).selectById(1L);
        verify(roomTypeRepository, times(1)).updateById(any(RoomType.class));
    }

    @Test
    @DisplayName("JSON序列化处理 - 空列表")
    void toStringJson_EmptyList() throws JsonProcessingException {
        // Act
        String result = roomTypeService.toStringJson(Collections.emptyList());

        // Assert
        assertNull(result);
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    @DisplayName("JSON序列化处理 - 正常列表")
    void toStringJson_NormalList() throws JsonProcessingException {
        // Arrange
        List<String> facilities = Arrays.asList("WiFi", "空调");
        when(objectMapper.writeValueAsString(facilities)).thenReturn("[\"WiFi\", \"空调\"]");

        // Act
        String result = roomTypeService.toStringJson(facilities);

        // Assert
        assertEquals("[\"WiFi\", \"空调\"]", result);
        verify(objectMapper, times(1)).writeValueAsString(facilities);
    }

    @Test
    @DisplayName("JSON序列化处理 - 异常")
    void toStringJson_Exception() throws JsonProcessingException {
        // Arrange
        List<String> facilities = Arrays.asList("WiFi", "空调");
        when(objectMapper.writeValueAsString(facilities))
                .thenThrow(new JsonProcessingException("JSON error") {});

        // Act
        String result = roomTypeService.toStringJson(facilities);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("JSON反序列化处理 - 空字符串")
    void toListFromJson_EmptyString() throws JsonProcessingException {
        // Act
        List<String> result = roomTypeService.toListFromJson("");

        // Assert
        assertNull(result);
        verify(objectMapper, never()).readValue(anyString(), any());
    }

    @Test
    @DisplayName("JSON反序列化处理 - null值")
    void toListFromJson_NullValue() throws JsonProcessingException {
        // Act
        List<String> result = roomTypeService.toListFromJson(null);

        // Assert
        assertNull(result);
        verify(objectMapper, never()).readValue(anyString(), any());
    }

    @Test
    @DisplayName("JSON反序列化处理 - 正常JSON")
    void toListFromJson_ValidJson() throws JsonProcessingException {
        // Arrange
        String json = "[\"WiFi\", \"空调\"]";
        List<String> expected = Arrays.asList("WiFi", "空调");
        when(objectMapper.readValue(json, any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(expected);

        // Act
        List<String> result = roomTypeService.toListFromJson(json);

        // Assert
        assertEquals(expected, result);
        verify(objectMapper, times(1)).readValue(json, any(com.fasterxml.jackson.core.type.TypeReference.class));
    }

    @Test
    @DisplayName("JSON反序列化处理 - 异常")
    void toListFromJson_Exception() throws JsonProcessingException {
        // Arrange
        String json = "invalid json";
        when(objectMapper.readValue(json, any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

        // Act
        List<String> result = roomTypeService.toListFromJson(json);

        // Assert
        assertNull(result);
    }
}
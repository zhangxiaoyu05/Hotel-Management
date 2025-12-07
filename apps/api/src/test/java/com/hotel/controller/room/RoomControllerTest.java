package com.hotel.controller.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.controller.BaseControllerTest;
import com.hotel.dto.room.*;
import com.hotel.entity.Room;
import com.hotel.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 房间控制器测试
 */
@ExtendWith(MockitoExtension.class)
public class RoomControllerTest extends BaseControllerTest {

    @MockBean
    private RoomService roomService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createRoom_Success() throws Exception {
        // Given
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("101");
        request.setRoomTypeId(1L);
        request.setFloor(1);
        request.setArea(25);
        request.setPrice(new BigDecimal("299.00"));
        request.setStatus("AVAILABLE");

        RoomResponse response = new RoomResponse();
        response.setId(1L);
        response.setRoomNumber("101");
        response.setRoomTypeId(1L);
        response.setFloor(1);
        response.setArea(25);
        response.setPrice(new BigDecimal("299.00"));
        response.setStatus("AVAILABLE");
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());

        when(roomService.createRoom(any(CreateRoomRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/rooms")
                        .with(authenticatedAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roomNumber").value("101"))
                .andExpect(jsonPath("$.data.price").value(299.00));
    }

    @Test
    void getRooms_Success() throws Exception {
        // Given
        RoomListResponse response = new RoomListResponse();
        response.setContent(Arrays.asList(
                createMockRoom(1L, "101"),
                createMockRoom(2L, "102")
        ));
        response.setTotalElements(2L);
        response.setTotalPages(1);
        response.setSize(20);
        response.setNumber(0);

        when(roomService.getRoomsWithPage(eq(0), eq(20), isNull(), isNull(),
                                         isNull(), eq("roomNumber"), eq("ASC")))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/rooms")
                        .with(authenticatedUser())
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "roomNumber")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getRoom_Success() throws Exception {
        // Given
        RoomResponse response = createMockRoom(1L, "101");

        when(roomService.getRoomById(1L))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/rooms/1")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roomNumber").value("101"));
    }

    @Test
    void updateRoom_Success() throws Exception {
        // Given
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setPrice(new BigDecimal("399.00"));

        RoomResponse response = createMockRoom(1L, "101");
        response.setPrice(new BigDecimal("399.00"));

        when(roomService.updateRoom(eq(1L), any(UpdateRoomRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/rooms/1")
                        .with(authenticatedAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.price").value(399.00));
    }

    @Test
    void deleteRoom_Success() throws Exception {
        // Given
        when(roomService.deleteRoom(1L))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(delete("/api/rooms/1")
                        .with(authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void batchUpdateRooms_Success() throws Exception {
        // Given
        BatchUpdateRequest request = new BatchUpdateRequest();
        request.setRoomIds(Arrays.asList(1L, 2L));
        request.setUpdates(new BatchUpdateRequest.UpdateContent());
        request.getUpdates().setStatus("MAINTENANCE");

        when(roomService.batchUpdateRooms(any(BatchUpdateRequest.class)))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/rooms/batch-update")
                        .with(authenticatedAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private RoomResponse createMockRoom(Long id, String roomNumber) {
        RoomResponse room = new RoomResponse();
        room.setId(id);
        room.setHotelId(1L);
        room.setRoomTypeId(1L);
        room.setRoomNumber(roomNumber);
        room.setFloor(1);
        room.setArea(25);
        room.setPrice(new BigDecimal("299.00"));
        room.setStatus("AVAILABLE");
        room.setImages(Arrays.asList("/images/room1.jpg"));
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        room.setRoomTypeName("标准间");
        return room;
    }
}
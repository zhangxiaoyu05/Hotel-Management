package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.room.*;
import com.hotel.entity.Room;
import com.hotel.entity.RoomType;
import com.hotel.enums.RoomStatus;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.exception.BusinessException;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 房间业务逻辑层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建房间
     */
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        // TODO: 从安全上下文获取当前用户的酒店ID
        Long hotelId = getCurrentUserHotelId();

        // 验证房间号唯一性
        Room existingRoom = roomRepository.findByRoomNumberAndHotelId(request.getRoomNumber(), hotelId);
        if (existingRoom != null) {
            throw new BusinessException("房间号已存在");
        }

        // 验证房间类型存在
        RoomType roomType = roomTypeRepository.selectById(request.getRoomTypeId());
        if (roomType == null || !roomType.getHotelId().equals(hotelId)) {
            throw new BusinessException("房间类型不存在");
        }

        Room room = new Room();
        room.setHotelId(hotelId);
        room.setRoomNumber(request.getRoomNumber());
        room.setRoomTypeId(request.getRoomTypeId());
        room.setFloor(request.getFloor());
        room.setArea(request.getArea());
        room.setPrice(request.getPrice());
        room.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : RoomStatus.AVAILABLE.name());

        // 处理图片列表
        room.setImageList(request.getImages());

        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        roomRepository.insert(room);
        return convertToRoomResponse(room, roomType.getName());
    }

    /**
     * 分页获取房间列表
     */
    public RoomListResponse getRoomsWithPage(Integer page, Integer size, String search, Long hotelId,
                                            String status, String sortBy, String sortDir) {
        RoomSearchRequest searchRequest = new RoomSearchRequest();
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setHotelId(hotelId);
        searchRequest.setStatus(status);
        searchRequest.setRoomNumber(search);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDir(sortDir);

        Page<Room> pageRequest = new Page<>(page, size);
        IPage<Room> roomPage = roomRepository.searchRooms(pageRequest, searchRequest);

        List<RoomResponse> roomResponses = roomPage.getRecords().stream()
                .map(room -> {
                    RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
                    return convertToRoomResponse(room, roomType != null ? roomType.getName() : null);
                })
                .collect(Collectors.toList());

        RoomListResponse response = new RoomListResponse();
        response.setContent(roomResponses);
        response.setTotalElements(roomPage.getTotal());
        response.setTotalPages(roomPage.getPages());
        response.setSize(roomPage.getSize());
        response.setNumber(roomPage.getCurrent());
        response.setFirst(roomPage.getCurrent() == 1);
        response.setLast(roomPage.getCurrent() == roomPage.getPages());
        response.setEmpty(roomPage.getRecords().isEmpty());

        return response;
    }

    /**
     * 根据ID获取房间
     */
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.selectById(id);
        if (room == null || room.getDeleted() == 1) {
            throw new ResourceNotFoundException("房间不存在");
        }

        RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
        return convertToRoomResponse(room, roomType != null ? roomType.getName() : null);
    }

    /**
     * 更新房间
     */
    @Transactional
    public RoomResponse updateRoom(Long id, UpdateRoomRequest request) {
        Room room = roomRepository.selectById(id);
        if (room == null || room.getDeleted() == 1) {
            throw new ResourceNotFoundException("房间不存在");
        }

        // 如果更新房间号，检查唯一性
        if (StringUtils.hasText(request.getRoomNumber()) && !request.getRoomNumber().equals(room.getRoomNumber())) {
            Room existingRoom = roomRepository.findByRoomNumberAndHotelId(request.getRoomNumber(), room.getHotelId());
            if (existingRoom != null) {
                throw new BusinessException("房间号已存在");
            }
            room.setRoomNumber(request.getRoomNumber());
        }

        // 如果更新房间类型，验证存在性
        if (request.getRoomTypeId() != null && !request.getRoomTypeId().equals(room.getRoomTypeId())) {
            RoomType roomType = roomTypeRepository.selectById(request.getRoomTypeId());
            if (roomType == null || !roomType.getHotelId().equals(room.getHotelId())) {
                throw new BusinessException("房间类型不存在");
            }
            room.setRoomTypeId(request.getRoomTypeId());
        }

        // 更新其他字段
        if (request.getFloor() != null) {
            room.setFloor(request.getFloor());
        }
        if (request.getArea() != null) {
            room.setArea(request.getArea());
        }
        if (request.getPrice() != null) {
            room.setPrice(request.getPrice());
        }
        if (StringUtils.hasText(request.getStatus())) {
            room.setStatus(request.getStatus());
        }

        // 处理图片更新
        room.setImageList(request.getImages());

        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.updateById(room);

        RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
        return convertToRoomResponse(room, roomType != null ? roomType.getName() : null);
    }

    /**
     * 删除房间
     */
    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.selectById(id);
        if (room == null || room.getDeleted() == 1) {
            throw new ResourceNotFoundException("房间不存在");
        }

        // 检查房间是否可以被删除（例如，是否有未完成的预订）
        if (RoomStatus.OCCUPIED.name().equals(room.getStatus())) {
            throw new BusinessException("已预订的房间不能删除");
        }

        room.setDeleted(1);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.updateById(room);
    }

    /**
     * 根据酒店ID获取房间列表
     */
    public List<RoomResponse> getRoomsByHotelId(Long hotelId) {
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return rooms.stream()
                .map(room -> {
                    RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
                    return convertToRoomResponse(room, roomType != null ? roomType.getName() : null);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据房间类型ID获取房间列表
     */
    public List<RoomResponse> getRoomsByRoomTypeId(Long roomTypeId) {
        List<Room> rooms = roomRepository.findByRoomTypeId(roomTypeId);
        return rooms.stream()
                .map(room -> {
                    RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
                    return convertToRoomResponse(room, roomType != null ? roomType.getName() : null);
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量更新房间
     */
    @Transactional
    public void batchUpdateRooms(BatchUpdateRequest request) {
        BatchUpdateRequest.UpdateContent updates = request.getUpdates();

        if (updates.getStatus() != null) {
            roomRepository.batchUpdateStatus(request.getRoomIds(), updates.getStatus());
        }

        if (updates.getPrice() != null) {
            roomRepository.batchUpdatePrice(request.getRoomIds(), updates.getPrice());
        }
    }

    /**
     * 搜索房间
     */
    public RoomListResponse searchRooms(RoomSearchRequest searchRequest) {
        Page<Room> pageRequest = new Page<>(searchRequest.getPage(), searchRequest.getSize());
        IPage<Room> roomPage = roomRepository.searchRooms(pageRequest, searchRequest);

        List<RoomResponse> roomResponses = roomPage.getRecords().stream()
                .map(room -> {
                    RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
                    return convertToRoomResponse(room, roomType != null ? roomType.getName() : null);
                })
                .collect(Collectors.toList());

        RoomListResponse response = new RoomListResponse();
        response.setContent(roomResponses);
        response.setTotalElements(roomPage.getTotal());
        response.setTotalPages(roomPage.getPages());
        response.setSize(roomPage.getSize());
        response.setNumber(roomPage.getCurrent());
        response.setFirst(roomPage.getCurrent() == 1);
        response.setLast(roomPage.getCurrent() == roomPage.getPages());
        response.setEmpty(roomPage.getRecords().isEmpty());

        return response;
    }

    /**
     * 转换为房间响应对象
     */
    private RoomResponse convertToRoomResponse(Room room, String roomTypeName) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setHotelId(room.getHotelId());
        response.setRoomTypeId(room.getRoomTypeId());
        response.setRoomNumber(room.getRoomNumber());
        response.setFloor(room.getFloor());
        response.setArea(room.getArea());
        response.setStatus(room.getStatus());
        response.setPrice(room.getPrice());
        response.setCreatedAt(room.getCreatedAt());
        response.setUpdatedAt(room.getUpdatedAt());
        response.setRoomTypeName(roomTypeName);

        // 解析图片
        response.setImages(room.getImageList());

        return response;
    }

    /**
     * 获取当前用户的酒店ID
     * @return 酒店ID
     */
    private Long getCurrentUserHotelId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // TODO: 从用户信息中获取酒店ID
            // 暂时返回默认值
            return 1L;
        }
        throw new BusinessException("用户未认证");
    }
}
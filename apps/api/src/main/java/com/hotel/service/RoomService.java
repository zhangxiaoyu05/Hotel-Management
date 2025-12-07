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
import com.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final HotelRepository hotelRepository;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper;
    private final UserContextService userContextService;

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
     * 获取房间在指定日期的价格
     * @param roomId 房间ID
     * @param date 日期
     * @return 计算后的价格
     */
    @Transactional(readOnly = true)
    public BigDecimal getRoomPriceForDate(Long roomId, LocalDate date) {
        return pricingService.calculateRoomPrice(roomId, date);
    }

    /**
     * 获取房间在日期范围内的价格
     * @param roomId 房间ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期到价格的映射
     */
    @Transactional(readOnly = true)
    public Map<LocalDate, BigDecimal> getRoomPricesForDateRange(Long roomId, LocalDate startDate, LocalDate endDate) {
        return pricingService.calculateRoomPricesForDateRange(roomId, startDate, endDate);
    }

    /**
     * 获取房间列表的价格信息
     * @param roomIds 房间ID列表
     * @param date 日期
     * @return 房间ID到价格的映射
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getRoomPricesForDate(List<Long> roomIds, LocalDate date) {
        return roomIds.stream()
                .collect(Collectors.toMap(
                        roomId -> roomId,
                        roomId -> pricingService.calculateRoomPrice(roomId, date)
                ));
    }

    /**
     * 批量更新房间价格并记录历史
     * @param roomIds 房间ID列表
     * @param newPrice 新价格
     * @param reason 变更原因
     */
    @Transactional
    public void batchUpdateRoomPrice(List<Long> roomIds, BigDecimal newPrice, String reason) {
        Long currentUserId = getCurrentUserId();

        for (Long roomId : roomIds) {
            Room room = roomRepository.selectById(roomId);
            if (room != null) {
                BigDecimal oldPrice = room.getPrice();

                // 更新房间价格
                room.setPrice(newPrice);
                roomRepository.updateById(room);

                // 记录价格变更历史
                RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
                if (roomType != null) {
                    pricingService.recordPriceChange(
                            room.getHotelId(),
                            room.getRoomTypeId(),
                            roomId,
                            oldPrice,
                            newPrice,
                            "MANUAL",
                            reason,
                            currentUserId
                    );
                }
            }
        }

        log.info("批量更新房间价格完成，房间数量: {}, 新价格: {}", roomIds.size(), newPrice);
    }

    /**
     * 扩展房间响应对象，包含价格信息
     */
    public RoomResponse getRoomWithPriceInfo(Long roomId, LocalDate date) {
        Room room = roomRepository.selectById(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("房间不存在: " + roomId);
        }

        RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
        RoomResponse response = convertToRoomResponse(room, roomType != null ? roomType.getName() : null);

        // 添加价格信息
        BigDecimal currentPrice = pricingService.calculateRoomPrice(roomId, date);
        response.setCalculatedPrice(currentPrice);

        // 添加价格变更信息
        if (room.getPrice() != null && !room.getPrice().equals(currentPrice)) {
            response.setPriceChanged(true);
            response.setPriceChange(currentPrice.subtract(room.getPrice()));
        }

        return response;
    }

    /**
     * 搜索可用房间
     */
    @Transactional(readOnly = true)
    public RoomSearchResultDto searchAvailableRooms(RoomSearchRequestDto request) {
        log.info("搜索可用房间，条件：{}", request);

        Page<Room> pageRequest = new Page<>(request.getPage(), request.getSize());

        // 搜索可用房间
        IPage<Room> roomPage = roomRepository.searchAvailableRooms(pageRequest, request);

        // 转换为响应DTO
        List<RoomSearchResponseDto> roomResponses = roomPage.getRecords().stream()
                .map(room -> convertToRoomSearchResponse(room, request))
                .collect(Collectors.toList());

        // 创建结果对象
        RoomSearchResultDto result = new RoomSearchResultDto(
                roomResponses,
                roomPage.getTotal(),
                request.getPage(),
                request.getSize()
        );

        log.info("搜索完成，找到{}个可用房间", roomResponses.size());
        return result;
    }

    /**
     * 转换为房间搜索响应DTO
     */
    private RoomSearchResponseDto convertToRoomSearchResponse(Room room, RoomSearchRequestDto searchRequest) {
        RoomSearchResponseDto response = new RoomSearchResponseDto();

        // 基本信息
        response.setId(room.getId());
        response.setRoomNumber(room.getRoomNumber());
        response.setFloor(room.getFloor());
        response.setArea(room.getArea());
        response.setStatus(room.getStatus());
        response.setPrice(room.getPrice());
        response.setImages(room.getImageList());
        response.setCreatedAt(room.getCreatedAt());
        response.setUpdatedAt(room.getUpdatedAt());

        // 房间类型信息
        RoomType roomType = roomTypeRepository.selectById(room.getRoomTypeId());
        if (roomType != null) {
            response.setRoomTypeId(roomType.getId());
            response.setRoomTypeName(roomType.getName());
            response.setRoomTypeCapacity(roomType.getCapacity());
            response.setRoomTypeDescription(roomType.getDescription());
            response.setRoomTypeFacilities(roomType.getFacilities());
        }

        // 酒店信息
        Hotel hotel = hotelRepository.selectById(room.getHotelId());
        if (hotel != null) {
            response.setHotelId(hotel.getId());
            response.setHotelName(hotel.getName());
            response.setHotelAddress(hotel.getAddress());
            response.setHotelPhone(hotel.getPhone());
            response.setHotelDescription(hotel.getDescription());
            response.setHotelFacilities(hotel.getFacilities());
            response.setHotelImages(hotel.getImages());
        }

        // 计算价格信息
        try {
            BigDecimal totalPrice = pricingService.calculateTotalPrice(
                    room.getId(),
                    searchRequest.getCheckInDate(),
                    searchRequest.getCheckOutDate()
            );
            response.setTotalPrice(totalPrice);

            // 计算平均每晚价格
            long nights = java.time.temporal.ChronoUnit.DAYS.between(
                    searchRequest.getCheckInDate(),
                    searchRequest.getCheckOutDate()
            );
            if (nights > 0) {
                response.setAveragePricePerNight(totalPrice.divide(BigDecimal.valueOf(nights), 2, BigDecimal.ROUND_HALF_UP));
            }
        } catch (Exception e) {
            log.warn("计算房间价格失败，使用基础价格，房间ID: {}", room.getId(), e);
            response.setTotalPrice(room.getPrice());
            response.setAveragePricePerNight(room.getPrice());
        }

        // 设置可用性信息
        response.setIsAvailable(true);
        response.setAvailableRooms(1);
        response.setAvailabilityStatus("可用");

        return response;
    }

    /**
     * 获取当前用户的酒店ID
     * @return 酒店ID
     */
    private Long getCurrentUserHotelId() {
        return userContextService.getCurrentUserHotelId();
    }
}
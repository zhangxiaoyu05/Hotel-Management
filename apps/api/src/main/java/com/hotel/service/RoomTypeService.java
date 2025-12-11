package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.roomtype.*;
import com.hotel.entity.RoomType;
import com.hotel.enums.RoomTypeStatus;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.util.XssSanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 房间类型业务逻辑层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelService hotelService;
    private final ObjectMapper objectMapper;

    // 本地缓存，用于存储已序列化的 JSON 字符串
    private final ConcurrentHashMap<String, String> jsonCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> listCache = new ConcurrentHashMap<>();

    // 缓存大小限制
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * 创建房间类型
     */
    @Transactional
    public RoomTypeResponse createRoomType(CreateRoomTypeRequest request) {
        // 验证酒店是否存在
        hotelService.getHotelById(request.getHotelId());

        // XSS 防护 - 清理用户输入
        String sanitizedName = XssSanitizer.sanitize(request.getName());
        String sanitizedDescription = XssSanitizer.sanitizeWithBasicHtml(request.getDescription());
        String sanitizedIconUrl = XssSanitizer.sanitizeUrl(request.getIconUrl());

        // 验证清理后的数据
        if (StringUtils.isBlank(sanitizedName)) {
            throw new RuntimeException("房间类型名称不能为空");
        }

        // 检查同一酒店下房间类型名称是否已存在
        RoomType existingRoomType = roomTypeRepository.selectByHotelIdAndName(
                request.getHotelId(), sanitizedName
        );
        if (existingRoomType != null) {
            throw new RuntimeException("该酒店下房间类型名称已存在");
        }

        // 清理设施列表中的每个项目
        List<String> sanitizedFacilities = null;
        if (request.getFacilities() != null) {
            sanitizedFacilities = request.getFacilities().stream()
                    .map(facility -> StringUtils.isNotBlank(facility) ? XssSanitizer.sanitize(facility) : "")
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
        }

        RoomType roomType = new RoomType();
        roomType.setHotelId(request.getHotelId());
        roomType.setName(sanitizedName);
        roomType.setCapacity(request.getCapacity());
        roomType.setBasePrice(request.getBasePrice());
        roomType.setFacilities(toStringJson(sanitizedFacilities));
        roomType.setDescription(sanitizedDescription);
        roomType.setIconUrl(sanitizedIconUrl);
        roomType.setStatus(RoomTypeStatus.ACTIVE.name());

        roomTypeRepository.insert(roomType);

        // 清理缓存
        clearCache();

        return convertToResponse(roomType);
    }

    /**
     * 更新房间类型
     */
    @Transactional
    public RoomTypeResponse updateRoomType(Long id, UpdateRoomTypeRequest request) {
        RoomType roomType = getRoomTypeEntityById(id);

        // XSS 防护 - 清理用户输入
        String sanitizedName = request.getName() != null ? XssSanitizer.sanitize(request.getName()) : null;
        String sanitizedDescription = request.getDescription() != null ? XssSanitizer.sanitizeWithBasicHtml(request.getDescription()) : null;
        String sanitizedIconUrl = request.getIconUrl() != null ? XssSanitizer.sanitizeUrl(request.getIconUrl()) : null;

        // 检查房间类型名称是否与其他房间类型重复
        if (sanitizedName != null && !sanitizedName.equals(roomType.getName())) {
            if (StringUtils.isBlank(sanitizedName)) {
                throw new RuntimeException("房间类型名称不能为空");
            }
            RoomType existingRoomType = roomTypeRepository.selectByHotelIdAndName(
                    roomType.getHotelId(), sanitizedName
            );
            if (existingRoomType != null && !existingRoomType.getId().equals(id)) {
                throw new RuntimeException("该酒店下房间类型名称已存在");
            }
        }

        // 清理设施列表中的每个项目
        List<String> sanitizedFacilities = null;
        if (request.getFacilities() != null) {
            sanitizedFacilities = request.getFacilities().stream()
                    .map(facility -> StringUtils.isNotBlank(facility) ? XssSanitizer.sanitize(facility) : "")
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
        }

        // 更新字段
        if (sanitizedName != null) {
            roomType.setName(sanitizedName);
        }
        if (request.getCapacity() != null) {
            roomType.setCapacity(request.getCapacity());
        }
        if (request.getBasePrice() != null) {
            roomType.setBasePrice(request.getBasePrice());
        }
        if (request.getFacilities() != null) {
            roomType.setFacilities(toStringJson(sanitizedFacilities));
        }
        if (sanitizedDescription != null) {
            roomType.setDescription(sanitizedDescription);
        }
        if (sanitizedIconUrl != null) {
            roomType.setIconUrl(sanitizedIconUrl);
        }
        if (request.getStatus() != null) {
            roomType.setStatus(request.getStatus().name());
        }

        roomTypeRepository.updateById(roomType);

        // 清理缓存
        clearCache();

        return convertToResponse(roomType);
    }

    /**
     * 删除房间类型
     */
    @Transactional
    public void deleteRoomType(Long id) {
        RoomType roomType = getRoomTypeEntityById(id);

        // 检查是否有关联的房间
        Integer roomCount = roomTypeRepository.countAssociatedRooms(id);
        if (roomCount > 0) {
            throw new RuntimeException("该房间类型下还有房间，无法删除");
        }

        roomTypeRepository.deleteById(id);
    }

    /**
     * 根据ID获取房间类型（缓存2小时）
     */
    @Cacheable(value = "room-types", key = "#id", unless = "#result == null")
    public RoomTypeResponse getRoomTypeById(Long id) {
        log.debug("从数据库获取房间类型信息: roomTypeId={}", id);
        RoomType roomType = getRoomTypeEntityById(id);
        return convertToResponse(roomType);
    }

    /**
     * 分页查询房间类型列表
     */
    public RoomTypeListResponse getRoomTypesWithPage(int page, int size, String search,
                                                   Long hotelId, String status,
                                                   String sortBy, String sortDir) {
        Page<RoomType> pageParam = new Page<>(page, size);
        IPage<RoomType> roomTypePage = roomTypeRepository.selectRoomTypesWithPage(
                pageParam, search, hotelId, status, sortBy, sortDir
        );

        RoomTypeListResponse response = new RoomTypeListResponse();
        response.setContent(roomTypePage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        response.setTotalElements(roomTypePage.getTotal());
        response.setTotalPages((int) roomTypePage.getPages());
        response.setSize((int) roomTypePage.getSize());
        response.setNumber((int) roomTypePage.getCurrent());
        response.setFirst(roomTypePage.getCurrent() == 1);
        response.setLast(roomTypePage.getCurrent() == roomTypePage.getPages());
        response.setHasNext(roomTypePage.hasNext());
        response.setHasPrevious(roomTypePage.hasPrevious());

        return response;
    }

    /**
     * 根据酒店ID获取房间类型列表
     */
    public List<RoomTypeResponse> getRoomTypesByHotelId(Long hotelId) {
        // 验证酒店是否存在
        hotelService.getHotelById(hotelId);

        List<RoomType> roomTypes = roomTypeRepository.selectByHotelId(hotelId);
        return roomTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据状态获取房间类型列表
     */
    public List<RoomTypeResponse> getRoomTypesByStatus(String status) {
        List<RoomType> roomTypes = roomTypeRepository.selectByStatus(status);
        return roomTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 更新房间类型状态
     */
    @Transactional
    public RoomTypeResponse updateRoomTypeStatus(Long id, RoomTypeStatus status) {
        RoomType roomType = getRoomTypeEntityById(id);
        roomType.setStatus(status.name());
        roomTypeRepository.updateById(roomType);
        return convertToResponse(roomType);
    }

    /**
     * 根据ID获取房间类型实体
     */
    private RoomType getRoomTypeEntityById(Long id) {
        RoomType roomType = roomTypeRepository.selectById(id);
        if (roomType == null) {
            throw new RuntimeException("房间类型不存在");
        }
        return roomType;
    }

    /**
     * 转换为响应DTO
     */
    private RoomTypeResponse convertToResponse(RoomType roomType) {
        RoomTypeResponse response = new RoomTypeResponse();
        response.setId(roomType.getId());
        response.setHotelId(roomType.getHotelId());
        response.setName(roomType.getName());
        response.setCapacity(roomType.getCapacity());
        response.setBasePrice(roomType.getBasePrice());
        response.setFacilities(toListFromJson(roomType.getFacilities()));
        response.setDescription(roomType.getDescription());
        response.setIconUrl(roomType.getIconUrl());
        response.setStatus(roomType.getStatus());
        response.setCreatedAt(roomType.getCreatedAt());
        response.setUpdatedAt(roomType.getUpdatedAt());

        // 获取酒店名称
        try {
            var hotel = hotelService.getHotelById(roomType.getHotelId());
            response.setHotelName(hotel.getName());
        } catch (Exception e) {
            log.warn("Failed to get hotel name for room type: {}", roomType.getId(), e);
        }

        return response;
    }

    /**
     * 将List转换为JSON字符串（带缓存）
     */
    private String toStringJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        // 生成缓存键
        String cacheKey = generateListCacheKey(list);

        // 尝试从缓存获取
        String cachedJson = jsonCache.get(cacheKey);
        if (cachedJson != null) {
            return cachedJson;
        }

        // 缓存未命中，执行序列化
        try {
            String json = objectMapper.writeValueAsString(list);

            // 添加到缓存（控制缓存大小）
            if (jsonCache.size() < MAX_CACHE_SIZE) {
                jsonCache.put(cacheKey, json);
            }

            return json;
        } catch (JsonProcessingException e) {
            log.error("Failed to convert list to JSON", e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为List（带缓存）
     */
    private List<String> toListFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        // 尝试从缓存获取
        List<String> cachedList = listCache.get(json);
        if (cachedList != null) {
            return cachedList;
        }

        // 缓存未命中，执行反序列化
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});

            // 添加到缓存（控制缓存大小）
            if (listCache.size() < MAX_CACHE_SIZE) {
                listCache.put(json, list);
            }

            return list;
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to list", e);
            return null;
        }
    }

    /**
     * 生成列表的缓存键
     */
    private String generateListCacheKey(List<String> list) {
        // 使用列表内容生成哈希作为缓存键
        return String.valueOf(list.hashCode());
    }

    /**
     * 清理缓存（在数据更新时调用）
     */
    private void clearCache() {
        if (jsonCache.size() > MAX_CACHE_SIZE * 0.8) {
            jsonCache.clear();
            listCache.clear();
            log.debug("Cleared JSON serialization cache");
        }
    }
}
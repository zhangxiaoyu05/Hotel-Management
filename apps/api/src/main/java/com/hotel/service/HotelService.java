package com.hotel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.dto.hotel.*;
import com.hotel.dto.facility.FacilityCategoryResponse;
import com.hotel.dto.facility.FacilityResponse;
import com.hotel.entity.Hotel;
import com.hotel.enums.HotelStatus;
import com.hotel.repository.HotelRepository;
import com.hotel.service.FacilityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final ObjectMapper objectMapper;
    private final FacilityService facilityService;

    @Transactional
    public HotelResponse createHotel(CreateHotelRequest request, Long createdBy) {
        // 检查酒店名称是否已存在
        Hotel existingHotel = hotelRepository.selectByName(request.getName());
        if (existingHotel != null) {
            throw new RuntimeException("酒店名称已存在");
        }

        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());
        hotel.setPhone(request.getPhone());
        hotel.setDescription(request.getDescription());
        hotel.setFacilities(toStringJson(request.getFacilities()));
        hotel.setImages(toStringJson(request.getImages()));
        hotel.setStatus(HotelStatus.ACTIVE.name());
        hotel.setCreatedBy(createdBy);

        hotelRepository.insert(hotel);

        return convertToResponse(hotel);
    }

    @Transactional(readOnly = true)
    public HotelListResponse getHotels(int page, int size, String search, String status, String sortBy, String sortDir) {
        Page<Hotel> pageParam = new Page<>(page, size);

        IPage<Hotel> hotelPage = hotelRepository.selectHotelsWithPage(
                pageParam, search, status, sortBy, sortDir
        );

        HotelListResponse response = new HotelListResponse();
        response.setContent(hotelPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        response.setTotalElements(hotelPage.getTotal());
        response.setTotalPages(hotelPage.getPages());
        response.setSize((int) hotelPage.getSize());
        response.setNumber((int) hotelPage.getCurrent());
        response.setFirst(hotelPage.getCurrent() == 1);
        response.setLast(hotelPage.getCurrent() == hotelPage.getPages());
        response.setNumberOfElements((int) hotelPage.getRecords().size());

        return response;
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotelById(Long id) {
        Hotel hotel = hotelRepository.selectById(id);
        if (hotel == null || hotel.getDeleted() == 1) {
            throw new RuntimeException("酒店不存在");
        }
        return convertToResponse(hotel);
    }

    @Transactional
    public HotelResponse updateHotel(Long id, UpdateHotelRequest request) {
        Hotel hotel = hotelRepository.selectById(id);
        if (hotel == null || hotel.getDeleted() == 1) {
            throw new RuntimeException("酒店不存在");
        }

        // 如果更新名称，检查名称是否重复
        if (request.getName() != null && !request.getName().equals(hotel.getName())) {
            Hotel existingHotel = hotelRepository.selectByName(request.getName());
            if (existingHotel != null && !existingHotel.getId().equals(id)) {
                throw new RuntimeException("酒店名称已存在");
            }
        }

        if (request.getName() != null) {
            hotel.setName(request.getName());
        }
        if (request.getAddress() != null) {
            hotel.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            hotel.setPhone(request.getPhone());
        }
        if (request.getDescription() != null) {
            hotel.setDescription(request.getDescription());
        }
        if (request.getFacilities() != null) {
            hotel.setFacilities(toStringJson(request.getFacilities()));
        }
        if (request.getImages() != null) {
            hotel.setImages(toStringJson(request.getImages()));
        }
        if (request.getStatus() != null) {
            hotel.setStatus(request.getStatus());
        }

        hotelRepository.updateById(hotel);

        return convertToResponse(hotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        Hotel hotel = hotelRepository.selectById(id);
        if (hotel == null || hotel.getDeleted() == 1) {
            throw new RuntimeException("酒店不存在");
        }

        hotel.setDeleted(1);
        hotelRepository.updateById(hotel);
    }

    @Transactional
    public HotelResponse updateHotelStatus(Long id, UpdateHotelStatusRequest request) {
        Hotel hotel = hotelRepository.selectById(id);
        if (hotel == null || hotel.getDeleted() == 1) {
            throw new RuntimeException("酒店不存在");
        }

        hotel.setStatus(request.getStatus());
        hotelRepository.updateById(hotel);

        return convertToResponse(hotel);
    }

    /**
     * 获取酒店设施分类
     */
    @Transactional(readOnly = true)
    public List<FacilityCategoryResponse> getHotelFacilityCategories(Long hotelId) {
        return facilityService.getActiveCategoriesByHotel(hotelId);
    }

    /**
     * 获取酒店所有设施
     */
    @Transactional(readOnly = true)
    public List<FacilityResponse> getHotelFacilities(Long hotelId) {
        return facilityService.getFacilitiesByHotel(hotelId);
    }

    /**
     * 获取酒店特色设施
     */
    @Transactional(readOnly = true)
    public List<FacilityResponse> getHotelFeaturedFacilities(Long hotelId) {
        return facilityService.getFeaturedFacilities(hotelId);
    }

    private HotelResponse convertToResponse(Hotel hotel) {
        HotelResponse response = new HotelResponse();
        response.setId(hotel.getId());
        response.setName(hotel.getName());
        response.setAddress(hotel.getAddress());
        response.setPhone(hotel.getPhone());
        response.setDescription(hotel.getDescription());
        response.setFacilities(parseStringJson(hotel.getFacilities()));
        response.setImages(parseStringJson(hotel.getImages()));
        response.setStatus(hotel.getStatus());
        response.setCreatedBy(hotel.getCreatedBy());
        response.setCreatedAt(hotel.getCreatedAt());
        response.setUpdatedAt(hotel.getUpdatedAt());
        return response;
    }

    private String toStringJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert list to JSON", e);
            return null;
        }
    }

    private List<String> parseStringJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to list", e);
            return List.of();
        }
    }
}
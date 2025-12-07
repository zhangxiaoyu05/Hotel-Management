package com.hotel.controller;

import com.hotel.dto.UploadResponse;
import com.hotel.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({FileController.class, FileResourceController.class})
@DisplayName("酒店图片上传测试")
class HotelImageUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    private MockMultipartFile validImageFile;
    private MockMultipartFile invalidImageFile;
    private UploadResponse uploadResponse;

    @BeforeEach
    void setUp() {
        // 创建有效的图片文件
        validImageFile = new MockMultipartFile(
                "file",
                "hotel-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        // 创建无效的图片文件（非图片格式）
        invalidImageFile = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf content".getBytes()
        );

        // 创建上传响应
        uploadResponse = new UploadResponse();
        uploadResponse.setSuccess(true);
        uploadResponse.setFileName("hotel-image.jpg");
        uploadResponse.setFilePath("/uploads/hotels/hotel-image.jpg");
        uploadResponse.setFileSize(1024L);
        uploadResponse.setMessage("文件上传成功");
    }

    @Test
    @DisplayName("上传酒店图片 - 成功")
    @WithMockUser(roles = "ADMIN")
    void uploadHotelImage_Success() throws Exception {
        // Given
        when(fileService.uploadHotelImage(any(MockMultipartFile.class), anyLong()))
                .thenReturn(uploadResponse);

        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .file(validImageFile)
                        .param("hotelId", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("文件上传成功"))
                .andExpect(jsonPath("$.data.fileName").value("hotel-image.jpg"))
                .andExpect(jsonPath("$.data.filePath").value("/uploads/hotels/hotel-image.jpg"));
    }

    @Test
    @DisplayName("上传酒店图片 - 无管理员权限")
    @WithMockUser(roles = "USER")
    void uploadHotelImage_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .file(validImageFile)
                        .param("hotelId", "1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("上传酒店图片 - 未认证")
    void uploadHotelImage_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .file(validImageFile)
                        .param("hotelId", "1")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("上传酒店图片 - 无效文件格式")
    @WithMockUser(roles = "ADMIN")
    void uploadHotelImage_InvalidFileType() throws Exception {
        // Given
        when(fileService.uploadHotelImage(any(MockMultipartFile.class), anyLong()))
                .thenThrow(new RuntimeException("不支持的文件格式，仅支持 JPEG、PNG、GIF 格式的图片"));

        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .file(invalidImageFile)
                        .param("hotelId", "1")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("不支持的文件格式，仅支持 JPEG、PNG、GIF 格式的图片"));
    }

    @Test
    @DisplayName("上传酒店图片 - 文件过大")
    @WithMockUser(roles = "ADMIN")
    void uploadHotelImage_FileTooLarge() throws Exception {
        // Given
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                largeContent
        );

        when(fileService.uploadHotelImage(any(MockMultipartFile.class), anyLong()))
                .thenThrow(new RuntimeException("文件大小不能超过 5MB"));

        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .file(largeFile)
                        .param("hotelId", "1")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("文件大小不能超过 5MB"));
    }

    @Test
    @DisplayName("上传酒店图片 - 缺少文件参数")
    @WithMockUser(roles = "ADMIN")
    void uploadHotelImage_MissingFile() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .param("hotelId", "1")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请选择要上传的文件"));
    }

    @Test
    @DisplayName("上传酒店图片 - 缺少酒店ID参数")
    @WithMockUser(roles = "ADMIN")
    void uploadHotelImage_MissingHotelId() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .file(validImageFile)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("酒店ID不能为空"));
    }

    @Test
    @DisplayName("上传酒店图片 - 无效酒店ID")
    @WithMockUser(roles = "ADMIN")
    void uploadHotelImage_InvalidHotelId() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .file(validImageFile)
                        .param("hotelId", "invalid")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("获取酒店图片 - 成功")
    void getHotelImage_Success() throws Exception {
        // Given
        String imagePath = "/uploads/hotels/hotel-image.jpg";
        byte[] imageContent = "test image content".getBytes();

        when(fileService.getFileContent(imagePath)).thenReturn(imageContent);
        when(fileService.getContentType(imagePath)).thenReturn(MediaType.IMAGE_JPEG_VALUE);

        // When & Then
        mockMvc.perform(get("/api/uploads" + imagePath))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE))
                .andExpect(content().bytes(imageContent));
    }

    @Test
    @DisplayName("获取酒店图片 - 文件不存在")
    void getHotelImage_NotFound() throws Exception {
        // Given
        String nonExistentPath = "/uploads/hotels/non-existent.jpg";

        when(fileService.getFileContent(nonExistentPath))
                .thenThrow(new RuntimeException("文件不存在"));

        // When & Then
        mockMvc.perform(get("/api/uploads" + nonExistentPath))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("删除酒店图片 - 成功")
    @WithMockUser(roles = "ADMIN")
    void deleteHotelImage_Success() throws Exception {
        // Given
        String imagePath = "/uploads/hotels/hotel-image.jpg";
        when(fileService.deleteFile(imagePath)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/uploads" + imagePath)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("文件删除成功"));
    }

    @Test
    @DisplayName("删除酒店图片 - 无管理员权限")
    @WithMockUser(roles = "USER")
    void deleteHotelImage_Forbidden() throws Exception {
        // Given
        String imagePath = "/uploads/hotels/hotel-image.jpg";

        // When & Then
        mockMvc.perform(delete("/api/uploads" + imagePath)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("删除酒店图片 - 文件不存在")
    @WithMockUser(roles = "ADMIN")
    void deleteHotelImage_NotFound() throws Exception {
        // Given
        String nonExistentPath = "/uploads/hotels/non-existent.jpg";
        when(fileService.deleteFile(nonExistentPath)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/uploads" + nonExistentPath)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("文件不存在"));
    }

    @Test
    @DisplayName("批量上传酒店图片 - 成功")
    @WithMockUser(roles = "ADMIN")
    void uploadMultipleHotelImages_Success() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "hotel-image1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content 1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "hotel-image2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content 2".getBytes()
        );

        List<UploadResponse> responses = Arrays.asList(uploadResponse, uploadResponse);
        when(fileService.uploadMultipleHotelImages(any(MockMultipartFile[].class), anyLong()))
                .thenReturn(responses);

        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-images")
                        .file(file1)
                        .file(file2)
                        .param("hotelId", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("上传酒店图片 - 包含恶意内容")
    @WithMockUser(roles = "ADMIN")
    void uploadHotelImage_MaliciousContent() throws Exception {
        // Given
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "malicious.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "<script>alert('xss')</script>".getBytes()
        );

        when(fileService.uploadHotelImage(any(MockMultipartFile.class), anyLong()))
                .thenThrow(new RuntimeException("检测到恶意文件内容"));

        // When & Then
        mockMvc.perform(multipart("/api/upload/hotel-image")
                        .file(maliciousFile)
                        .param("hotelId", "1")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("检测到恶意文件内容"));
    }
}
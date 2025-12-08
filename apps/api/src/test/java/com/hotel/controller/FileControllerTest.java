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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@DisplayName("文件控制器测试")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    private MockMultipartFile validImageFile;
    private MockMultipartFile invalidFile;
    private MockMultipartFile oversizedFile;

    @BeforeEach
    void setUp() {
        // 有效的图片文件
        validImageFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // 无效的文件类型
        invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test text content".getBytes()
        );

        // 过大的文件 (超过5MB)
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        oversizedFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        // Mock file service response
        UploadResponse uploadResponse = new UploadResponse();
        uploadResponse.setUrl("http://localhost:8080/api/files/upload-url/test.jpg");
        uploadResponse.setFilename("test.jpg");
        uploadResponse.setOriginalFilename("test.jpg");
        uploadResponse.setContentType("image/jpeg");
        uploadResponse.setSize(validImageFile.getSize());

        when(fileService.uploadFile(any())).thenReturn(uploadResponse);
    }

    @Test
    @WithMockUser
    @DisplayName("上传有效图片文件 - 成功")
    void uploadFile_ValidImage_Success() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                        .file(validImageFile)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("文件上传成功"))
                .andExpect(jsonPath("$.data.url").value("http://localhost:8080/api/files/upload-url/test.jpg"))
                .andExpect(jsonPath("$.data.filename").value("test.jpg"))
                .andExpect(jsonPath("$.data.contentType").value("image/jpeg"));
    }

    @Test
    @WithMockUser
    @DisplayName("上传无效文件类型 - 失败")
    void uploadFile_InvalidFileType_Failure() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                        .file(invalidFile)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只支持 JPG、JPEG、PNG 格式的图片文件"));
    }

    @Test
    @WithMockUser
    @DisplayName("上传过大文件 - 失败")
    void uploadFile_OversizedFile_Failure() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                        .file(oversizedFile)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("文件大小不能超过 5MB"));
    }

    @Test
    @DisplayName("上传文件 - 未认证")
    void uploadFile_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                        .file(validImageFile)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取文件URL - 成功")
    void getFileUrl_Success() throws Exception {
        // Given
        String fileName = "test.jpg";
        String expectedUrl = "http://localhost:8080/api/files/upload-url/" + fileName;
        when(fileService.getFileUrl(fileName)).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(get("/api/files/upload-url/{fileName}", fileName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取文件URL成功"))
                .andExpect(jsonPath("$.data").value(expectedUrl));
    }
}
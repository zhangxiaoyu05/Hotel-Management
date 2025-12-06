package com.hotel.dto;

import lombok.Data;

@Data
public class UploadResponse {
    private String url;
    private String filename;
    private String originalFilename;
    private String contentType;
    private long size;
}
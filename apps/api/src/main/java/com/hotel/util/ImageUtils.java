package com.hotel.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;

@Slf4j
public class ImageUtils {

    // 支持的图片格式
    private static final String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "gif", "webp"};

    // 压缩质量 (0.0 - 1.0)
    private static final float COMPRESSION_QUALITY = 0.8f;

    // 最大宽度
    private static final int MAX_WIDTH = 1200;

    // 最大高度
    private static final int MAX_HEIGHT = 1200;

    /**
     * 检查文件是否为支持的图片格式
     */
    public static boolean isSupportedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        return Arrays.asList(SUPPORTED_FORMATS).contains(extension);
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 压缩图片
     * @param file 原始图片文件
     * @return 压缩后的图片字节数组
     */
    public static byte[] compressImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (!isSupportedImage(file)) {
            throw new IllegalArgumentException("不支持的图片格式");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new IllegalArgumentException("无法读取图片内容");
            }

            // 计算压缩后的尺寸
            int[] newDimensions = calculateNewDimensions(
                originalImage.getWidth(),
                originalImage.getHeight()
            );

            BufferedImage compressedImage = new BufferedImage(
                newDimensions[0],
                newDimensions[1],
                BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g2d = compressedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制压缩后的图片
            g2d.drawImage(originalImage, 0, 0, newDimensions[0], newDimensions[1], null);
            g2d.dispose();

            // 转换为字节数组
            String formatName = getFormatName(file.getOriginalFilename());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 对于JPEG格式，使用压缩质量
            if ("jpg".equalsIgnoreCase(formatName) || "jpeg".equalsIgnoreCase(formatName)) {
                // 使用ImageWriter来控制JPEG压缩质量
                javax.imageio.ImageWriter writer =
                    javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
                javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();

                if (param.canWriteCompressed()) {
                    param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(COMPRESSION_QUALITY);
                }

                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new javax.imageio.IIOImage(compressedImage, null, null), param);
                }
                writer.dispose();
            } else {
                ImageIO.write(compressedImage, formatName, baos);
            }

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("图片压缩失败: {}", e.getMessage(), e);
            throw new IOException("图片压缩失败", e);
        }
    }

    /**
     * 计算新的图片尺寸
     */
    private static int[] calculateNewDimensions(int originalWidth, int originalHeight) {
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // 如果图片尺寸超过最大值，进行等比缩放
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            double widthRatio = (double) MAX_WIDTH / originalWidth;
            double heightRatio = (double) MAX_HEIGHT / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            newWidth = (int) (originalWidth * ratio);
            newHeight = (int) (originalHeight * ratio);
        }

        return new int[]{newWidth, newHeight};
    }

    /**
     * 获取图片格式名称
     */
    private static String getFormatName(String filename) {
        if (filename == null) {
            return "jpg";
        }

        String extension = getFileExtension(filename).toLowerCase();

        // 处理特殊情况
        switch (extension) {
            case "jpg":
                return "jpg";
            case "jpeg":
                return "jpg";
            case "png":
                return "png";
            case "gif":
                return "gif";
            case "webp":
                return "webp";
            default:
                return "jpg"; // 默认使用JPEG格式
        }
    }

    /**
     * 验证图片文件
     */
    public static void validateImageFile(MultipartFile file, long maxSize) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }

        // 检查文件大小
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("图片大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }

        // 检查文件类型
        if (!isSupportedImage(file)) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、GIF、WebP 格式的图片");
        }

        // 验证图片内容
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("无效的图片文件");
            }
        }
    }

    /**
     * 获取图片信息
     */
    public static ImageInfo getImageInfo(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("无法读取图片内容");
            }

            return ImageInfo.builder()
                .width(image.getWidth())
                .height(image.getHeight())
                .format(getFormatName(file.getOriginalFilename()))
                .size(file.getSize())
                .build();
        }
    }

    /**
     * 图片信息类
     */
    @lombok.Data
    @lombok.Builder
    public static class ImageInfo {
        private int width;
        private int height;
        private String format;
        private long size;
    }
}
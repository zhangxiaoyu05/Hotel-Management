package com.hotel.dto.room;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量更新房间请求 DTO
 */
@Data
public class BatchUpdateRequest {

    @NotEmpty(message = "房间ID列表不能为空")
    private List<Long> roomIds;

    @NotNull(message = "更新内容不能为空")
    @Valid
    private UpdateContent updates;

    @Data
    public static class UpdateContent {
        private String status;
        private java.math.BigDecimal price;
    }
}
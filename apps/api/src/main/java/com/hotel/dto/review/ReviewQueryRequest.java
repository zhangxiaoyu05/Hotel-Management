package com.hotel.dto.review;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

@Data
public class ReviewQueryRequest {

    @NotNull(message = "酒店ID不能为空")
    private Long hotelId;

    private Long roomId;

    @Min(value = 1, message = "最低评分不能小于1")
    @Max(value = 5, message = "最低评分不能大于5")
    private Integer minRating;

    @Min(value = 1, message = "最高评分不能小于1")
    @Max(value = 5, message = "最高评分不能大于5")
    private Integer maxRating;

    private Boolean hasImages;

    private String sortBy = "date"; // date, rating

    private String sortOrder = "desc"; // asc, desc

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页大小必须大于0")
    private Integer size = 10;
}
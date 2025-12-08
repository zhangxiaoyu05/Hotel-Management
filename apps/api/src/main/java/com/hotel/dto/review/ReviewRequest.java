package com.hotel.dto.review;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class ReviewRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Min(value = 1, message = "总体评分最低为1星")
    @Max(value = 5, message = "总体评分最高为5星")
    private Integer overallRating;

    @Min(value = 1, message = "清洁度评分最低为1星")
    @Max(value = 5, message = "清洁度评分最高为5星")
    private Integer cleanlinessRating;

    @Min(value = 1, message = "服务评分最低为1星")
    @Max(value = 5, message = "服务评分最高为5星")
    private Integer serviceRating;

    @Min(value = 1, message = "设施评分最低为1星")
    @Max(value = 5, message = "设施评分最高为5星")
    private Integer facilitiesRating;

    @Min(value = 1, message = "位置评分最低为1星")
    @Max(value = 5, message = "位置评分最高为5星")
    private Integer locationRating;

    @Size(max = 1000, message = "评价内容最多1000字符")
    private String comment;

    private List<String> images;

    private Boolean isAnonymous = false;
}
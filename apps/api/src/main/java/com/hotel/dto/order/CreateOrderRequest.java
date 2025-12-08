package com.hotel.dto.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@Data
public class CreateOrderRequest {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "入住日期不能为空")
    private LocalDate checkInDate;

    @NotNull(message = "退房日期不能为空")
    private LocalDate checkOutDate;

    @NotNull(message = "入住人数不能为空")
    @Positive(message = "入住人数必须大于0")
    private Integer guestCount;

    @NotBlank(message = "入住人姓名不能为空")
    private String guestName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号码")
    private String guestPhone;

    @Email(message = "请输入正确的邮箱格式")
    private String guestEmail;

    private String specialRequests;

    private String couponCode;
}
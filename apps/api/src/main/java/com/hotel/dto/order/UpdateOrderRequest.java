package com.hotel.dto.order;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UpdateOrderRequest {

    @Size(max = 100, message = "入住人姓名长度不能超过100个字符")
    private String guestName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
    private String guestPhone;

    @Email(message = "请输入有效的邮箱地址")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String guestEmail;

    @Size(max = 500, message = "特殊要求长度不能超过500个字符")
    private String specialRequests;
}
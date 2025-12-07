package com.hotel.dto.hotel;

import com.hotel.validation.Phone;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class CreateHotelRequest {

    @NotBlank(message = "酒店名称不能为空")
    @Size(min = 2, max = 100, message = "酒店名称长度为2-100个字符")
    private String name;

    @NotBlank(message = "酒店地址不能为空")
    @Size(min = 5, max = 255, message = "酒店地址长度为5-255个字符")
    private String address;

    @Phone(message = "请输入有效的电话号码")
    private String phone;

    @Size(max = 1000, message = "酒店简介不能超过1000个字符")
    private String description;

    private List<String> facilities;

    private List<String> images;
}
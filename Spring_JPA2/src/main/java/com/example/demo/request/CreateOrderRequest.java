package com.example.demo.request;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "客戶 ID 不可為空")
    @Size(max = 36, message = "客戶 ID 最多 36 字")
    private String customerId;

    @Size(max = 255, message = "收件地址最多 255 字")
    private String shippingAddress;

    @NotEmpty(message = "訂單至少需要一項商品")
    private Map<String, @NotNull(message = "數量不可為空") @Positive(message = "數量必須大於 0") Integer> productQuantities = new HashMap<>();

}

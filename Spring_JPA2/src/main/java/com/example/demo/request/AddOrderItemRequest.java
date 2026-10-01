package com.example.demo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

@Data
public class AddOrderItemRequest {
    @NotBlank(message = "商品 ID 不可為空")
    private String productId;

    @NotNull(message = "數量不可為空")
    @Positive(message = "數量必須大於 0")
    private Integer quantity;
}

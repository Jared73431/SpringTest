package com.example.demo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

/**
 * 「在既有訂單加入商品」的 Request 物件，搭配 Controller 的 @Valid 進行 Bean Validation。
 * 與 Response DTO 分開定義，只接收用戶端可以決定的欄位（單價由伺服器從商品取得，不讓用戶端傳入）。
 */
@Data
public class AddOrderItemRequest {
    @NotBlank(message = "商品 ID 不可為空")
    private String productId;

    // @Positive 不檢查 null，所以必填還需要 @NotNull
    @NotNull(message = "數量不可為空")
    @Positive(message = "數量必須大於 0")
    private Integer quantity;
}

package com.example.demo.request;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * 建立訂單的 Request 物件：以 {@code Map<商品 ID, 數量>} 一次傳入多項商品。
 * 只包含用戶端可決定的欄位；訂單 ID、狀態、日期、單價與總金額都由伺服器產生或計算。
 */
@Data
public class CreateOrderRequest {

    @NotBlank(message = "客戶 ID 不可為空")
    @Size(max = 36, message = "客戶 ID 最多 36 字")
    private String customerId;

    @Size(max = 255, message = "收件地址最多 255 字")
    private String shippingAddress;

    // 驗證註解寫在泛型參數上（container element constraint，Bean Validation 2.0 起支援），
    // 可以逐一檢查 Map 中每個「數量」值，而不只是檢查 Map 本身
    @NotEmpty(message = "訂單至少需要一項商品")
    private Map<String, @NotNull(message = "數量不可為空") @Positive(message = "數量必須大於 0") Integer> productQuantities = new HashMap<>();

}

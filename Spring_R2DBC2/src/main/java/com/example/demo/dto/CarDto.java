package com.example.demo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarDto {

    private Long id;

    // 長度對應資料表的 VARCHAR(50) / VARCHAR(30)：修正前沒有限制，太長的值寫入時才失敗，回傳 500
    @NotBlank(message = "Make is required")
    @Size(max = 50, message = "Make must be at most 50 characters")
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model must be at most 50 characters")
    private String model;

    @NotNull(message = "Year is required")
    // 修正前是 @Min(1900)，但資料庫的 CHECK 是 year > 1900：1900 通過驗證，寫入時才失敗。訊息本來就寫「大於 1900」
    @Min(value = 1901, message = "Year must be greater than 1900")
    @Max(value = 2100, message = "Year must be less than 2100")
    private Integer year;

    @Size(max = 30, message = "Color must be at most 30 characters")
    private String color;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    // 對應 DECIMAL(10, 2)：整數最多 8 位、小數最多 2 位
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimals")
    private BigDecimal price;
}

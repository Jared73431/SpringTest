package com.example.demo.request;

import lombok.Data;

@Data
public class AddOrderItemRequest {
    private String productId;
    private Integer quantity;
}

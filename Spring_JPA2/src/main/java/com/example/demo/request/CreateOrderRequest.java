package com.example.demo.request;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class CreateOrderRequest {

    private String customerId;
    private String shippingAddress;
    private Map<String, Integer> productQuantities = new HashMap<>();

}

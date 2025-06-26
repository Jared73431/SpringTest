package com.example.demo.batch.dto;

public record  HealthInsuranceDTO (
        String fileName,
        String payType,
        String nationalNo,
        String cifNo,
        String withholdDate,
        String withhold,
        String reasonType
) {
}

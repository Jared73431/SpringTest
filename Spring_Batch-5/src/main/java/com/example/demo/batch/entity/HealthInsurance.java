package com.example.demo.batch.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "healthInsurance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthInsurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String payType;
    private String nationalNo;
    private String cifNo;
    private String withholdDate;
    private String withhold;
    private String reasonType;

    // 提供一个便于从DTO转换的构造函数
    public HealthInsurance(String fileName,
                           String payType,
                           String nationalNo,
                           String cifNo,
                           String withholdDate,
                           String withhold,
                           String reasonType) {
        this.fileName = fileName;
        this.payType = payType;
        this.nationalNo = nationalNo;
        this.cifNo = cifNo;
        this.withholdDate = withholdDate;
        this.withhold = withhold;
        this.reasonType = reasonType;
    }
}

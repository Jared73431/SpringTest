package com.example.demo.batch.writer;

import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.stereotype.Component;

import com.example.demo.batch.entity.HealthInsurance;
import com.example.demo.batch.repository.HealthInsuranceRepository;

@Component
public class HealthInsuranceItemWriter {

    private final HealthInsuranceRepository healthInsuranceRepository;

    public HealthInsuranceItemWriter(HealthInsuranceRepository healthInsuranceRepository){
        this.healthInsuranceRepository = healthInsuranceRepository;
    }

    public RepositoryItemWriter<HealthInsurance> createWriter() {
        return new RepositoryItemWriterBuilder<HealthInsurance>()
                .repository(healthInsuranceRepository)
                .methodName("save")
                .build();
    }
}

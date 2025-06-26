package com.example.demo.batch.reader;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.example.demo.batch.dto.HealthInsuranceDTO;

@Component
public class HealthInsuranceItemReader {

    public FlatFileItemReader<HealthInsuranceDTO> createReader() {
        return new FlatFileItemReaderBuilder<HealthInsuranceDTO>()
                .name("healthInsuranceItemReader")
                .resource(new ClassPathResource("UpdateHealthInsurancePremiumFromNH.csv"))
                .delimited()
                .delimiter(",")
                .quoteCharacter('"') // 指定引號字符，因為您的數據用引號包圍
                .names(Arrays.stream(HealthInsuranceDTO.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new))
                .linesToSkip(1) // 跳過標題行
                .targetType(HealthInsuranceDTO.class)
                .build();
    }
}

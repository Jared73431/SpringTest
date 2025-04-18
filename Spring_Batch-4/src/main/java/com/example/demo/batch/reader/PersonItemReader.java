package com.example.demo.batch.reader;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.example.demo.batch.dto.PersonDTO;

@Component
public class PersonItemReader{

    public FlatFileItemReader<PersonDTO> createReader() {
        return new FlatFileItemReaderBuilder<PersonDTO>()
                .name("personItemReader")
                .resource(new ClassPathResource("sample-data.csv"))
                .delimited()
                .names(Arrays.stream(PersonDTO.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toArray(String[]::new))
                .targetType(PersonDTO.class)
                .build();
    }
}

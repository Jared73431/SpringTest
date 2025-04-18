package com.example.demo.batch.writer;

import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.stereotype.Component;

import com.example.demo.batch.entity.Person;
import com.example.demo.batch.repository.PersonRepository;

@Component
public class PersonItemWriter {

    private final PersonRepository repository;

    public PersonItemWriter(PersonRepository repository) {
        this.repository = repository;
    }

    public RepositoryItemWriter<Person> createWriter() {
        return new RepositoryItemWriterBuilder<Person>()
                .repository(repository)
                .methodName("save")
                .build();
    }
}

package com.example.demo.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.example.demo.batch.dto.Person;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class PersonItemProcessor implements ItemProcessor<Person,Person> {
    @Override
    public Person process(final Person person) throws Exception {
        final String firstName = person.firstName().toUpperCase();
        final String lastName = person.lastName().toUpperCase();

        final Person transformedPerson = new Person(firstName, lastName);

        log.info("Converting ("+ person + ") into (" + transformedPerson + ")");

        return transformedPerson;
    }
}

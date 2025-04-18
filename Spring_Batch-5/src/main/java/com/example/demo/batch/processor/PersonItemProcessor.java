package com.example.demo.batch.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.demo.batch.dto.PersonDTO;
import com.example.demo.batch.entity.Person;

import lombok.extern.log4j.Log4j2;

/**
 * 功能：实现了ItemProcessor接口，定义数据处理逻辑
 * 处理内容：将人名转换为大写形式
 * 日志：记录转换前后的数据对比
 */
@Log4j2
@Component
public class PersonItemProcessor implements ItemProcessor<PersonDTO,Person> {
    @Override
    public Person process(final PersonDTO person) throws Exception {
        final String firstName = person.firstName().toUpperCase();
        final String lastName = person.lastName().toUpperCase();

        final Person transformedPerson = new Person(firstName, lastName);

        log.info("Converting ("+ person + ") into (" + transformedPerson + ")");

        return transformedPerson;
    }

}

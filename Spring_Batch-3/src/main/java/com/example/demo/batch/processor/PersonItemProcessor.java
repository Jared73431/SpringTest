package com.example.demo.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.example.demo.batch.dto.PersonDTO;
import com.example.demo.batch.model.Person;

/**
 * Processor：把姓名轉成大寫。
 *
 * <p>
 * 輸入（PersonDTO，CSV 的一行）和輸出（Person，資料庫的一列）型別可以不同。回傳 null 表示「略過這一筆」，不會傳給 Writer。
 */
public class PersonItemProcessor implements ItemProcessor<PersonDTO, Person> {

	private static final Logger log = LoggerFactory.getLogger(PersonItemProcessor.class);

	@Override
	public Person process(PersonDTO person) {
		Person transformed = new Person(person.firstName().toUpperCase(), person.lastName().toUpperCase());
		log.info("Converting ({}) into ({})", person, transformed);
		return transformed;
	}

}

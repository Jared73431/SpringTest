package com.example.demo.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import lombok.Data;
import lombok.ToString;

@Data
@Entity
@ToString
public class Book implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3392162000827962466L;

	@Id
	@SequenceGenerator(name="Book_id_GENERATOR", sequenceName="Book_id_seq", allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Book_id_GENERATOR")
	private Integer Id;
	
	private Integer ISBN;
	
	private String title;

	private String author;

	private Integer year;

	private String publisher;

	private double cost;
}

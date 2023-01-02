package com.example.demo.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;

@Entity
@Data
public class Book implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2836661917894057082L;

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

package com.example.demo.entity;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table
@Data
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String task;

    @Column(insertable = false, columnDefinition = "int default 1")
    private Integer status = 1;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Date createTime = new Date();

    @LastModifiedDate
    @Column(nullable = false)
    private Date updateTime = new Date();

    @JsonBackReference
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="user_id")
    private User user;
}

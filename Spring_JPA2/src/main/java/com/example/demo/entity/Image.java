package com.example.demo.entity;

import java.util.Date;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String contentType;

    // 不使用 @Lob：Hibernate 在 PostgreSQL 會把 @Lob byte[] 存成 oid（Large Object），
    // 讀取時必須在交易中，刪除資料列也不會自動刪除 Large Object。一般的 byte[] 會對應到 bytea。
    private byte[] data;

    private Date uploadDate;

    // 預設建構子
    public Image() {
        this.uploadDate = new Date();
    }

    public Image(String name, String contentType, byte[] data) {
        this.name = name;
        this.contentType = contentType;
        this.data = data;
        this.uploadDate = new Date();
    }
}

package com.example.demo.entity;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Entity
@Data
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String contentType;

    @Lob
    @Column(length = 1000000)  // 設定適當的長度以儲存圖片
    private byte[] data;

    @Temporal(TemporalType.TIMESTAMP)
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

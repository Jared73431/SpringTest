package com.example.demo.entity;

import java.util.Date;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 上傳圖片的 JPA Entity，圖片二進位內容直接存在資料庫（PostgreSQL bytea 欄位）。
 * 列表查詢時不應載入 data，請使用 ImageRepository.findAllSummaries 取得不含內容的摘要。
 */
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

    // 預設建構子（JPA 規範要求 Entity 必須有無參數建構子，Hibernate 透過它以反射建立物件）
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

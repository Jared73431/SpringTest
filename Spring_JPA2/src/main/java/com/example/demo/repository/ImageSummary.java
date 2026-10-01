package com.example.demo.repository;

import java.util.Date;

/**
 * 圖片摘要的介面投影（Interface Projection）：Spring Data 依查詢結果的欄位別名對應到 getter。
 */
public interface ImageSummary {

    Long getId();

    String getName();

    String getContentType();

    Integer getSize();

    Date getUploadDate();
}

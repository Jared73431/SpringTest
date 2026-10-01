package com.example.demo.repository;

import java.util.Date;

/**
 * 圖片摘要的介面投影（Interface Projection）：Spring Data 依查詢結果的欄位別名對應到 getter。
 * 只需宣告 getter，Spring Data 會在執行時期產生代理物件，因此只查需要的欄位，不必載入整個 Entity。
 */
public interface ImageSummary {

    Long getId();

    String getName();

    String getContentType();

    Integer getSize();

    Date getUploadDate();
}

package com.example.demo.dto;

/**
 * 新增 / 修改書籍的 Request Body，原樣轉送給 Server。
 * 刻意不在 Client 重複驗證：欄位規則只維護在 Server，驗證失敗時 Server 回 400，再由 Client 原樣轉回呼叫端。
 * 與 Server 的 BookRequest 是兩份獨立的類別，只要 JSON 欄位名稱一致即可（見 readme「DTO 要不要共用」）。
 */
public record BookRequest(
		Integer isbn,
		String title,
		String author,
		Integer year,
		String publisher,
		Double cost) {
}

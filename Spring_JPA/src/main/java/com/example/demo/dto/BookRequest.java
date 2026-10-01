package com.example.demo.dto;

import com.example.demo.entity.Book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 新增 / 修改書籍的 Request Body。所有欄位皆為必填。
 * 使用 Java record：自動產生建構子、accessor（isbn()）、equals / hashCode，適合不可變的資料傳遞物件。
 * 欄位上的 Bean Validation 註解（{@code @NotNull}、{@code @NotBlank}）搭配 Controller 的 {@code @Valid} 生效，
 * 驗證失敗時回 400。
 */
public record BookRequest(
		@NotNull Integer isbn,
		@NotBlank String title,
		@NotBlank String author,
		@NotNull Integer year,
		@NotBlank String publisher,
		@NotNull Double cost) {

	/** 轉成 Entity；id 不在 Request 中，由 Service 決定（新增時由 sequence 產生、修改時使用 URL 的 id） */
	public Book toEntity() {
		Book book = new Book();
		book.setISBN(isbn);
		book.setTitle(title);
		book.setAuthor(author);
		book.setYear(year);
		book.setPublisher(publisher);
		book.setCost(cost);
		return book;
	}
}

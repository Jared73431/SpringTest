package com.example.demo.lesson;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 第 4 課用的 Entity：同一張 category 表，用三種寫法對應，比較 save() 的行為。
 *
 * <p>
 * Spring Data 的 save() 用 isNew() 決定要 INSERT 還是 UPDATE。預設的判斷：<b>id 是 null 才是新的</b>。
 * 主鍵由程式自己指定（代碼、UUID、身分證字號…）時，id 一開始就有值，save() 會以為是既有資料而執行 UPDATE。
 */
final class LessonCategories {

	private LessonCategories() {
	}

	/** 一般寫法：record 也可以當 R2DBC 的 Entity（主鍵由程式指定，不需要從資料庫回填任何值） */
	@Table("category")
	record Category(@Id String code, String name, CategoryStatus status) {
	}

	/** 解法一：實作 Persistable，自己決定 isNew() */
	@Table("category")
	static class PersistableCategory implements Persistable<String> {

		@Id
		private final String code;

		private final String name;

		private final CategoryStatus status;

		// 不是資料表的欄位；從資料庫讀出來時是 false，用 newCategory() 建立時是 true
		@Transient
		private boolean isNew;

		PersistableCategory(String code, String name, CategoryStatus status) {
			this.code = code;
			this.name = name;
			this.status = status;
		}

		static PersistableCategory newCategory(String code, String name) {
			var category = new PersistableCategory(code, name, CategoryStatus.ACTIVE);
			category.isNew = true;
			return category;
		}

		@Override
		public String getId() {
			return code;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		String name() {
			return name;
		}
	}

	/** 解法二：加上 @Version。有版本欄位時，Spring Data 改用「version 是不是 null」判斷新舊，順便得到樂觀鎖 */
	@Table("category")
	static class VersionedCategory {

		@Id
		private String code;

		private String name;

		private CategoryStatus status;

		@Version
		private Integer version;

		VersionedCategory(String code, String name, CategoryStatus status, Integer version) {
			this.code = code;
			this.name = name;
			this.status = status;
			this.version = version;
		}

		Integer version() {
			return version;
		}

		void rename(String name) {
			this.name = name;
		}
	}
}

package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * API 的 Request Body。每個都很小，集中在同一個檔案方便對照；驗證失敗時回 400 ProblemDetail。
 */
public final class Requests {

	private Requests() {
	}

	/** 設定字串；ttlSeconds 為 null 表示不過期 */
	public record StringValue(@NotNull String value, @Positive Long ttlSeconds) {
	}

	/** List、Hash 欄位等單一值 */
	public record Value(@NotNull String value) {
	}

	public record SetMembers(@NotEmpty List<@NotBlank String> members) {
	}

	public record ScoredMember(@NotBlank String member, @NotNull Double score) {
	}

	public record Ttl(@Positive long seconds) {
	}

	public record UserRequest(@NotBlank String name, @Email String email, @PositiveOrZero Integer age) {
	}
}

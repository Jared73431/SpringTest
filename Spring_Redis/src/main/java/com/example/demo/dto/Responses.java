package com.example.demo.dto;

/**
 * API 的 Response Body。修正前大多回傳中文字串（例如「設置成功: ...」），呼叫端難以解析，改為 JSON。
 */
public final class Responses {

	private Responses() {
	}

	/** ttlSeconds：剩餘秒數；null 表示不會過期 */
	public record StringValue(String key, String value, Long ttlSeconds) {
	}

	public record Value(String value) {
	}

	public record Membership(String member, boolean isMember) {
	}

	public record ScoredMember(String member, double score) {
	}

	public record KeyInfo(String key, Long ttlSeconds) {
	}
}

package com.example.demo.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import com.example.demo.exception.KeyNotFoundException;

/**
 * 情境一：排行榜（ZSet）。
 *
 * ZSet 會依分數自動排序，加分、查名次、取前 N 名都是 O(log N)，
 * 用資料庫做排行榜通常要 ORDER BY 整張表，資料量大時很慢，這是 Redis 最經典的用途之一。
 *
 * <pre>
 * 加分       ZINCRBY leaderboard:{board} {points} {player}   （玩家不存在時從 0 開始）
 * 前 N 名    ZREVRANGE leaderboard:{board} 0 N-1 WITHSCORES   （REV：分數由高到低）
 * 查名次     ZREVRANK  leaderboard:{board} {player}           （從 0 開始）
 * </pre>
 */
@Service
public class LeaderboardService {

	public record Entry(long rank, String player, double score) {
	}

	private final StringRedisTemplate redis;

	public LeaderboardService(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public Entry addPoints(String board, String player, double points) {
		redis.opsForZSet().incrementScore(key(board), player, points);
		return find(board, player);
	}

	public List<Entry> top(String board, int count) {
		Set<TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(key(board), 0, count - 1);
		List<Entry> entries = new ArrayList<>();
		long rank = 1;
		for (TypedTuple<String> tuple : tuples) {
			entries.add(new Entry(rank++, tuple.getValue(), tuple.getScore()));
		}
		return entries;
	}

	public Entry find(String board, String player) {
		Long rank = redis.opsForZSet().reverseRank(key(board), player);
		Double score = redis.opsForZSet().score(key(board), player);
		if (rank == null || score == null) {
			throw new KeyNotFoundException("Player '" + player + "' on leaderboard '" + board + "'");
		}
		// Redis 的名次從 0 開始，對外顯示從 1 開始
		return new Entry(rank + 1, player, score);
	}

	private static String key(String board) {
		return "leaderboard:" + board;
	}
}

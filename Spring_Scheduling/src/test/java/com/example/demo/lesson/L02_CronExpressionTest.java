package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

/**
 * 第 2 課：cron 運算式與時區。
 *
 * <p>
 * 用 CronExpression.next() 直接算出「下一次什麼時候執行」：不必等待，也不必真的等到凌晨 3 點，就能驗證 cron 寫得對不對。
 *
 * <pre>
 * Spring 的 cron 有 6 個欄位：秒 分 時 日 月 星期（Linux crontab 只有 5 個，沒有秒）
 * 0 30 22 * * *       每天 22:30:00
 * 0 0 9 * * MON-FRI   平日 09:00
 * 0 0 23 L * *        每月最後一天 23:00
 * \@daily / @hourly    巨集
 * </pre>
 */
class L02_CronExpressionTest {

	private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
	private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");

	private static ZonedDateTime taipei(int year, int month, int day, int hour, int minute) {
		return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, TAIPEI);
	}

	@Test
	void next_shouldReturnTodayIfTimeHasNotPassed() {
		var cron = CronExpression.parse("0 30 22 * * *");

		assertThat(cron.next(taipei(2026, 10, 8, 21, 0))).isEqualTo(taipei(2026, 10, 8, 22, 30));
		assertThat(cron.next(taipei(2026, 10, 8, 23, 0))).isEqualTo(taipei(2026, 10, 9, 22, 30));
	}

	@Test
	void weekdays_shouldSkipWeekend() {
		var cron = CronExpression.parse("0 0 9 * * MON-FRI");

		// 2026-10-10 是星期六 → 下一次是星期一 10-12
		assertThat(cron.next(taipei(2026, 10, 10, 12, 0))).isEqualTo(taipei(2026, 10, 12, 9, 0));
	}

	@Test
	void lastDayOfMonth_shouldHandleFebruary() {
		var cron = CronExpression.parse("0 0 23 L * *");

		assertThat(cron.next(taipei(2026, 2, 10, 0, 0))).isEqualTo(taipei(2026, 2, 28, 23, 0));
	}

	// 從 Linux crontab 複製過來最常見的錯誤：少了「秒」，Spring 啟動時就會失敗
	@Test
	void fiveFieldCron_shouldBeRejected() {
		assertThatThrownBy(() -> CronExpression.parse("*/5 * * * *"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must consist of 6 fields");
	}

	// 同一個 cron，時區不同，實際執行的時間點相差 8 小時。@Scheduled 沒有指定 zone 時使用伺服器的時區
	// （容器裡通常是 UTC），所以排在「台北凌晨 3 點」的工作要寫 zone = "Asia/Taipei"
	@Test
	void zone_shouldChangeTheActualInstant() {
		var cron = CronExpression.parse("0 0 3 * * *");
		Instant now = Instant.parse("2026-10-08T00:00:00Z"); // 台北時間 08:00

		Instant inTaipei = cron.next(now.atZone(TAIPEI)).toInstant();
		Instant inUtc = cron.next(now.atZone(ZoneId.of("UTC"))).toInstant();

		assertThat(inTaipei).isEqualTo(Instant.parse("2026-10-08T19:00:00Z")); // 台北 10-09 03:00
		assertThat(inUtc).isEqualTo(Instant.parse("2026-10-08T03:00:00Z"));
	}

	// 夏令時間開始那天（美國 2026-03-08，02:00 直接跳到 03:00）：02:30 不存在，這一天的排程被跳過
	@Test
	void springForward_shouldSkipTheDay_whenTimeDoesNotExist() {
		var cron = CronExpression.parse("0 30 2 * * *");

		ZonedDateTime next = cron.next(ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, LOS_ANGELES));

		assertThat(next.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 9));
	}

	// 夏令時間結束那天（美國 2026-11-01，01:00～02:00 出現兩次）：01:30 出現兩次，排程執行兩次
	@Test
	void fallBack_shouldRunTwice_whenTimeOccursTwice() {
		var cron = CronExpression.parse("0 30 1 * * *");

		ZonedDateTime first = cron.next(ZonedDateTime.of(2026, 10, 31, 12, 0, 0, 0, LOS_ANGELES));
		ZonedDateTime second = cron.next(first);

		assertThat(first.toLocalDateTime()).isEqualTo(second.toLocalDateTime()); // 同樣是 11-01 01:30
		assertThat(first.getOffset()).isNotEqualTo(second.getOffset()); // -07:00 與 -08:00
	}

	// 「打烊後 30 分鐘」：23:45 打烊的門市在隔天 00:15 執行，但處理的是「前一天」的營業資料
	@Test
	void closingPlus30Minutes_shouldBelongToThePreviousBusinessDay_whenCrossingMidnight() {
		LocalDateTime closing = LocalDateTime.of(2026, 10, 8, 23, 45);
		LocalDateTime runAt = closing.plusMinutes(30);

		assertThat(runAt.toLocalTime()).isEqualTo(LocalTime.of(0, 15));
		assertThat(runAt.toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 9));
		assertThat(runAt.minusMinutes(30).toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 8)); // 營業日
	}
}

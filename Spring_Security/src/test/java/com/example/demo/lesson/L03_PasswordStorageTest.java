package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 第 3 課：密碼怎麼存？
 *
 * <ul>
 * <li>設定檔的 spring.security.user.password 是<b>明碼</b>：Spring Boot 在前面加上 {noop}（不編碼）</li>
 * <li>正式系統要存<b>雜湊值</b>（hash），而且要用 BCrypt 這種刻意算得很慢的演算法</li>
 * <li>雜湊不能還原：登入時把輸入的密碼再算一次，比對結果是否相同</li>
 * </ul>
 *
 * 大括號裡的 {noop}、{bcrypt} 告訴 Spring Security 這個密碼是用什麼方式存的。
 */
@SpringBootTest
class L03_PasswordStorageTest {

	@Autowired
	private UserDetailsService userDetailsService;

	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@Test
	void configuredPassword_shouldBeKeptInPlainText() {
		assertThat(userDetailsService.loadUserByUsername("user").getPassword()).isEqualTo("{noop}password");
	}

	@Test
	void bcrypt_shouldStoreAHash_thatCannotBeTurnedBackIntoThePassword() {
		String hash = passwordEncoder.encode("password");

		assertThat(hash).startsWith("{bcrypt}$2a$10$").doesNotContain("password");
		assertThat(passwordEncoder.matches("password", hash)).isTrue();
		assertThat(passwordEncoder.matches("wrong", hash)).isFalse();
	}

	// 每次產生不同的雜湊值：BCrypt 會加上隨機的 salt（存在雜湊值裡）。
	// 兩個使用者用同一個密碼，資料庫裡看起來也不一樣，攻擊者不能用「事先算好的密碼表」一次破解
	@Test
	void bcrypt_shouldGiveADifferentHashEveryTime() {
		String first = passwordEncoder.encode("password");
		String second = passwordEncoder.encode("password");

		assertThat(first).isNotEqualTo(second);
		assertThat(passwordEncoder.matches("password", first)).isTrue();
		assertThat(passwordEncoder.matches("password", second)).isTrue();
	}
}

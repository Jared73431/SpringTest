package com.example.demo.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 自訂的 Security 設定。
 *
 * <p>
 * 必須有 {@code @Configuration}：修正前只標了 {@code @EnableWebSecurity}，Spring Security 6 起它不再包含
 * {@code @Configuration}，這個類別根本沒有被載入（baseline 測試確認），設定完全沒有作用。
 * Spring Boot 已經自動加上 {@code @EnableWebSecurity}，所以這裡不需要再寫。
 */
@Configuration
@EnableMethodSecurity // 啟用 @PreAuthorize（第 3 課）
public class SecurityConfig {

	/**
	 * 哪些路徑需要什麼權限（第 1 課）。規則<b>由上往下</b>比對，第一個符合的就決定結果，
	 * 所以範圍小的規則寫在前面，anyRequest() 一定放最後。
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/public/**").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				// 自訂 SecurityFilterChain 之後，Spring Boot 的預設就不再套用：要用的登入方式必須自己加回來
				.formLogin(Customizer.withDefaults())
				.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	/**
	 * 使用者存在資料庫（第 4 課）：Spring Security 內建的實作，讀 users、authorities 兩張表（Flyway 建立）。
	 */
	@Bean
	public JdbcUserDetailsManager userDetailsManager(DataSource dataSource) {
		return new JdbcUserDetailsManager(dataSource);
	}

	/**
	 * 依照密碼前面的 {bcrypt}、{noop} 選擇比對方式；新密碼用 BCrypt 編碼。
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}

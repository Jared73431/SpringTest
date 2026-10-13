package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 兩條 SecurityFilterChain：一個請求只會走第一條符合的 chain（@Order 小的先比對）。
 *
 * <pre>
 * @Order(1) /api/auth/token   用帳密（HTTP Basic）換一個 JWT
 * @Order(2) 其他所有路徑       只接受 Authorization: Bearer &lt;JWT&gt;
 * </pre>
 *
 * 兩條都是<b>無狀態</b>的：不建立 Session，每個請求都自己帶著身分（帳密或 Token）。
 * 所以也不需要 CSRF 防護：CSRF 利用的是「瀏覽器自動帶 cookie」，這裡完全不用 cookie。
 *
 * <p>
 * 修正前的設定是從 token 驗證的樣板改來的，但 JWT filter、登入 API 都是註解，也沒有使用者，需要登入的路徑任何人都進不去。
 */
@Configuration
public class SecurityConfig {

	@Bean
	@Order(1)
	public SecurityFilterChain tokenEndpointChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/api/auth/token")
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(csrf -> csrf.disable());
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						// Token 的 scope 是 "USER ADMIN" 時，Spring 自動轉成 SCOPE_USER、SCOPE_ADMIN 兩個權限
						.requestMatchers("/api/admin/**").hasAuthority("SCOPE_ADMIN")
						.anyRequest().authenticated())
				// OAuth2 Resource Server：從 Authorization: Bearer 取出 JWT，用 JwtDecoder（公鑰）驗證簽章與到期時間
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(csrf -> csrf.disable());
		return http.build();
	}

	/**
	 * 範例帳號（密碼都是 password）。存在記憶體中，讓這個模組專注在 Token；使用者存在資料庫見 Spring_Security2。
	 */
	@Bean
	public UserDetailsService users(PasswordEncoder passwordEncoder) {
		return new InMemoryUserDetailsManager(
				User.withUsername("alice").password(passwordEncoder.encode("password")).authorities("USER").build(),
				User.withUsername("admin").password(passwordEncoder.encode("password")).authorities("USER", "ADMIN")
						.build());
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}

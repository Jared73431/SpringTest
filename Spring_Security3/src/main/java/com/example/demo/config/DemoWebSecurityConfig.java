package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 升級時改寫：WebSecurityConfigurerAdapter 在 Spring Security 6 被移除，改成宣告 SecurityFilterChain Bean。
 * 行為和改寫前相同（baseline 測試確認）；JWT 驗證在重構時加入。
 *
 * <pre>
 * 改寫前（Spring Security 5）                     改寫後（Spring Security 6）
 * extends WebSecurityConfigurerAdapter            @Bean SecurityFilterChain
 * .csrf().disable()                               .csrf(csrf -&gt; csrf.disable())
 * .authorizeRequests().antMatchers(...)           .authorizeHttpRequests(auth -&gt; auth.requestMatchers(...))
 * .and()                                          每一項各自用一個 lambda，不需要 .and()
 * </pre>
 */
@Configuration
public class DemoWebSecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// CSRF 停用：不使用 Session
				.csrf(csrf -> csrf.disable())
				// 不建立 Session
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// 登入、驗證碼只允許「沒有登入」的人
						.requestMatchers("/login", "/captchaImage").anonymous()
						.requestMatchers(HttpMethod.GET, "/*.html", "/**/*.html", "/**/*.css", "/**/*.js").permitAll()
						.requestMatchers("/profile/**").permitAll()
						// 其他都需要登入
						.anyRequest().authenticated())
				.headers(headers -> headers.frameOptions(frame -> frame.disable()));
		return http.build();
	}
}

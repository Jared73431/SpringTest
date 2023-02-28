package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@EnableWebSecurity
public class DemoWebSecurityConfig {

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		// 仅仅作为演示
		return (web) -> web.ignoring().requestMatchers("/ignore1", "/ignore2");
//		HttpSecurity.authorizeHttpRequests
	}

//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//        .authorizeRequests()
//        .anyRequest()
//        .authenticated()
//        .and().formLogin()
//        .and().httpBasic()
//        .and().csrf().disable(); // <-- 關閉CSRF，請求時才不用另外帶CSRF token
//    }

//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception{
//    	
//    }

//    @Override
//    public void configure(WebSecurity web) throws Exception{
//    	
//    }
}

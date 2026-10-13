package com.example.demo.token;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * 簽發 JWT。Token 的內容（claims）只放標準欄位與角色：
 *
 * <pre>
 * iss    誰簽發的
 * sub    這個 Token 代表誰（帳號）
 * iat    什麼時候簽發
 * exp    什麼時候到期（15 分鐘後）
 * scope  有哪些權限（"USER ADMIN"）
 * </pre>
 *
 * 內容任何人都看得到（只是 Base64 編碼），所以不要放密碼、身分證字號這類敏感資料。
 */
@Service
public class TokenService {

	public static final String ISSUER = "spring-security3";

	public static final Duration TIME_TO_LIVE = Duration.ofMinutes(15);

	private final JwtEncoder jwtEncoder;

	public TokenService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	public String issue(Authentication authentication) {
		Instant now = Instant.now();
		String scope = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> !authority.startsWith("FACTOR_")) // Spring Security 7 加上的登入方式（見 Spring_Security2）
				.sorted()
				.collect(Collectors.joining(" "));
		var claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(authentication.getName())
				.issuedAt(now)
				.expiresAt(now.plus(TIME_TO_LIVE))
				.claim("scope", scope)
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
}

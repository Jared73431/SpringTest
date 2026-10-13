package com.example.demo.token;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用帳密換 Token：POST /api/auth/token（HTTP Basic 帶帳密）。
 *
 * <pre>
 * curl -X POST -u alice:password http://localhost:8107/api/auth/token
 * </pre>
 *
 * 之後的請求都改帶 Authorization: Bearer &lt;accessToken&gt;，不再傳帳密。
 */
@RestController
public class TokenController {

	private final TokenService tokenService;

	public TokenController(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
	}

	@PostMapping("/api/auth/token")
	public TokenResponse token(Authentication authentication) {
		return new TokenResponse(tokenService.issue(authentication), "Bearer",
				TokenService.TIME_TO_LIVE.toSeconds());
	}
}

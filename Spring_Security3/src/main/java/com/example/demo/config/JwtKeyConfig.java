package com.example.demo.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * JWT 的簽章金鑰：RSA 金鑰對。
 *
 * <pre>
 * 私鑰（private key）  簽發 Token 時用來簽章    只有簽發 Token 的服務有
 * 公鑰（public key）   驗證 Token 時用來檢查簽章  可以公開給任何要驗證 Token 的服務
 * </pre>
 *
 * 簽章不是加密：任何人都能讀 Token 的內容，但只要改了一個字，簽章就對不上（第 2 課）。
 *
 * <p>
 * 金鑰對在<b>啟動時產生</b>：不必把私鑰放進 repo（就算是練習用的也不放）。代價是重新啟動後，舊的 Token 全部失效，
 * 多台主機也不能共用。正式環境的私鑰放在密鑰管理服務；或交給 Keycloak 這類授權伺服器簽發 Token，
 * 這裡只保留 JwtDecoder，改成 NimbusJwtDecoder.withIssuerLocation(Keycloak 的網址)。
 */
@Configuration
public class JwtKeyConfig {

	@Bean
	public KeyPair jwtKeyPair() throws NoSuchAlgorithmException {
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}

	@Bean
	public JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
		return NimbusJwtEncoder.withKeyPair((RSAPublicKey) jwtKeyPair.getPublic(),
				(RSAPrivateKey) jwtKeyPair.getPrivate()).build();
	}

	@Bean
	public JwtDecoder jwtDecoder(KeyPair jwtKeyPair) {
		return NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic()).build();
	}
}

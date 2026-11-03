package com.devops_platform.backend.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    // HS256 requires a key of at least 256 bits (RFC 7518 §3.2).
    static final int MIN_SECRET_BYTES = 32;

    private final String jwtSecret;

    public JwtConfig(@Value("${JWT_SECRET}") String jwtSecret) {
        // Nimbus would only reject a short key when signing — i.e. at the first
        // login, after the app already passed its healthchecks. Fail at startup.
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long (HS256). "
                            + "Generate one with: openssl rand -base64 32");
        }
        this.jwtSecret = jwtSecret;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        // The JWK needs an explicit algorithm, otherwise NimbusJwtEncoder can't
        // pick a signing key for a header-less encode() call (JwtEncodingException:
        // "Failed to select a JWK signing key").
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKeyBytes())
                .algorithm(JWSAlgorithm.HS256)
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(secretKeyBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private byte[] secretKeyBytes() {
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }
}

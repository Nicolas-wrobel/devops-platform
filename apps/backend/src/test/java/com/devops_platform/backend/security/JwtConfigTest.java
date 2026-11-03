package com.devops_platform.backend.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtConfigTest {

    @Test
    void constructor_rejectsSecretShorterThan256Bits() {
        String tooShort = "a".repeat(JwtConfig.MIN_SECRET_BYTES - 1);

        assertThatThrownBy(() -> new JwtConfig(tooShort))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void constructor_rejectsEmptySecret() {
        assertThatThrownBy(() -> new JwtConfig(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_acceptsSecretOfExactly256Bits() {
        String minimal = "a".repeat(JwtConfig.MIN_SECRET_BYTES);

        assertThatCode(() -> new JwtConfig(minimal)).doesNotThrowAnyException();
    }
}

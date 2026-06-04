package com.smanioto.bank.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    // 64-byte base64 secret suitable for HS256/HS512
    private static final String SECRET = "TXlTdXBlclNlY3JldEtleUZvclNtYW5pb3RvQmFua0F1dGhTZXJ2aWNlMTIzNDU2Nzg=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600);
    }

    private UserDetails userOf(String username) {
        return User.withUsername(username).password("ignored").roles("USER").build();
    }

    @Test
    void generateToken_shouldProduceNonBlankToken() {
        var token = jwtService.generateToken(userOf("alice"));

        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_shouldReturnSubjectFromToken() {
        var token = jwtService.generateToken(userOf("alice"));

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isTokenValid_whenTokenMatchesUser_shouldReturnTrue() {
        var userDetails = userOf("bob");
        var token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_whenTokenBelongsToDifferentUser_shouldReturnFalse() {
        var token = jwtService.generateToken(userOf("alice"));

        assertThat(jwtService.isTokenValid(token, userOf("bob"))).isFalse();
    }

    @Test
    void isTokenValid_whenTokenIsExpired_shouldReturnFalse() {
        var expiredJwtService = new JwtService(SECRET, -1);
        var token = expiredJwtService.generateToken(userOf("carol"));

        assertThat(jwtService.isTokenValid(token, userOf("carol"))).isFalse();
    }

    @Test
    void isTokenValid_whenTokenIsMalformed_shouldReturnFalse() {
        assertThat(jwtService.isTokenValid("not.a.valid.token", userOf("alice"))).isFalse();
    }
}

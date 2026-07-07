package com.physio.infrastructure.in.security;

import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {

    private static final String SECRET_VALIDA = "0123456789abcdef0123456789abcdef";

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
    }

    private void setSecret(String secret) {
        ReflectionTestUtils.setField(tokenService, "secret", secret);
    }

    @Test
    void deveFalharNoBootQuandoSecretNulo() {
        setSecret(null);
        assertThrows(IllegalStateException.class, () -> tokenService.validarSecret());
    }

    @Test
    void deveFalharNoBootQuandoSecretVazia() {
        setSecret("   ");
        assertThrows(IllegalStateException.class, () -> tokenService.validarSecret());
    }

    @Test
    void deveFalharNoBootQuandoSecretMenorQue32Caracteres() {
        setSecret("secret-curta");
        assertThrows(IllegalStateException.class, () -> tokenService.validarSecret());
    }

    @Test
    void deveSubirNormalmenteComSecretValida() {
        setSecret(SECRET_VALIDA);
        assertDoesNotThrow(() -> tokenService.validarSecret());
    }

    @Test
    void deveGerarTokenEValidarRetornandoOSubject() {
        setSecret(SECRET_VALIDA);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setLogin("fisioterapeuta");

        String token = tokenService.generateToken(usuario);

        assertTrue(token != null && !token.isBlank());
        assertEquals(3, token.split("\\.").length, "JWT deve ter header.payload.signature");
        assertEquals("fisioterapeuta", tokenService.validateToken(token));
    }

    @Test
    void deveRetornarVazioParaTokenInvalido() {
        setSecret(SECRET_VALIDA);
        assertEquals("", tokenService.validateToken("token.invalido.aqui"));
    }

    @Test
    void deveRetornarVazioParaTokenAssinadoComOutraSecret() {
        setSecret(SECRET_VALIDA);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setLogin("fisioterapeuta");
        String token = tokenService.generateToken(usuario);

        setSecret("outra-secret-completamente-diferente-32c");
        assertEquals("", tokenService.validateToken(token));
    }

    @Test
    void deveRetornarVazioParaTokenExpirado() {
        setSecret(SECRET_VALIDA);
        com.auth0.jwt.algorithms.Algorithm algorithm = com.auth0.jwt.algorithms.Algorithm.HMAC256(SECRET_VALIDA);
        String tokenExpirado = com.auth0.jwt.JWT.create()
                .withIssuer("physio-manager")
                .withSubject("fisioterapeuta")
                .withExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .sign(algorithm);

        assertEquals("", tokenService.validateToken(tokenExpirado));
    }
}

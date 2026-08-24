package com.physio.infrastructure.in.security;

import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals("fisioterapeuta", tokenService.validateToken(token).orElseThrow());
    }

    @Test
    void deveRetornarVazioParaTokenInvalido() {
        setSecret(SECRET_VALIDA);
        assertTrue(tokenService.validateToken("token.invalido.aqui").isEmpty());
    }

    @Test
    void deveRetornarVazioParaTokenAssinadoComOutraSecret() {
        setSecret(SECRET_VALIDA);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setLogin("fisioterapeuta");
        String token = tokenService.generateToken(usuario);

        setSecret("outra-secret-completamente-diferente-32c");
        assertTrue(tokenService.validateToken(token).isEmpty());
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

        assertTrue(tokenService.validateToken(tokenExpirado).isEmpty());
    }

    @Test
    void naoDeveRenovarTokenLongeDaExpiracao() {
        setSecret(SECRET_VALIDA);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setLogin("fisioterapeuta");
        String token = tokenService.generateToken(usuario); // expira em 8h, limiar é 1h

        assertTrue(tokenService.renovarSeProximoDaExpiracao(token).isEmpty());
    }

    @Test
    void deveRenovarTokenProximoDaExpiracao() {
        setSecret(SECRET_VALIDA);
        com.auth0.jwt.algorithms.Algorithm algorithm = com.auth0.jwt.algorithms.Algorithm.HMAC256(SECRET_VALIDA);
        String tokenQuaseExpirando = com.auth0.jwt.JWT.create()
                .withIssuer("physio-manager")
                .withSubject("fisioterapeuta")
                .withExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .sign(algorithm);

        var novoToken = tokenService.renovarSeProximoDaExpiracao(tokenQuaseExpirando);

        assertTrue(novoToken.isPresent());
        assertEquals("fisioterapeuta", tokenService.validateToken(novoToken.get()).orElseThrow());
    }

    @Test
    void naoDeveRenovarTokenInvalido() {
        setSecret(SECRET_VALIDA);
        assertFalse(tokenService.renovarSeProximoDaExpiracao("token.invalido.aqui").isPresent());
    }
}

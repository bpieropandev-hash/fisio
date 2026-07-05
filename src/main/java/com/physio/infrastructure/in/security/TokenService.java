package com.physio.infrastructure.in.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private static final int TAMANHO_MINIMO_SECRET = 32;
    private static final Duration VALIDADE_TOKEN = Duration.ofHours(8);

    @Value("${api.security.token.secret}")
    private String secret;

    @PostConstruct
    void validarSecret() {
        // Falha no boot em vez de aceitar tokens forjaveis com secret ausente/fraca
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "API_SECURITY_TOKEN_SECRET nao definida. Configure a variavel de ambiente " +
                    "com um valor de no minimo " + TAMANHO_MINIMO_SECRET + " caracteres.");
        }
        if (secret.length() < TAMANHO_MINIMO_SECRET) {
            throw new IllegalStateException(
                    "API_SECURITY_TOKEN_SECRET muito curta (" + secret.length() + " caracteres). " +
                    "Use no minimo " + TAMANHO_MINIMO_SECRET + " caracteres.");
        }
    }

    public String generateToken(UsuarioEntity usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("physio-manager")
                    .withSubject(usuario.getLogin())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("physio-manager")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    private Instant genExpirationDate() {
        // Instant e absoluto: independe do fuso do servidor (Railway roda em UTC)
        return Instant.now().plus(VALIDADE_TOKEN);
    }
}

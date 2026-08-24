package com.physio.infrastructure.in.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class TokenService {

    private static final int TAMANHO_MINIMO_SECRET = 32;
    private static final Duration VALIDADE_TOKEN = Duration.ofHours(8);
    // Sliding expiration (P3.3): se faltar menos que isso pro token vencer,
    // SecurityFilter emite um token novo na resposta em vez de deixar a sessão cair.
    private static final Duration LIMIAR_RENOVACAO = Duration.ofHours(1);

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
        return generateToken(usuario.getLogin());
    }

    public String generateToken(String login) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("physio-manager")
                    .withSubject(login)
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    /**
     * @return subject do token se válido; Optional vazio se inválido/expirado/assinado com outra secret.
     */
    public Optional<String> validateToken(String token) {
        try {
            return Optional.of(verify(token).getSubject());
        } catch (JWTVerificationException exception) {
            return Optional.empty();
        }
    }

    /**
     * Sliding expiration: se o token for válido e estiver a menos de {@link #LIMIAR_RENOVACAO}
     * de expirar, gera um substituto com validade renovada. Caso contrário (inválido, expirado
     * ou ainda longe do vencimento), retorna vazio.
     */
    public Optional<String> renovarSeProximoDaExpiracao(String token) {
        try {
            DecodedJWT decoded = verify(token);
            Instant expiraEm = decoded.getExpiresAt().toInstant();
            if (Duration.between(Instant.now(), expiraEm).compareTo(LIMIAR_RENOVACAO) < 0) {
                return Optional.of(generateToken(decoded.getSubject()));
            }
            return Optional.empty();
        } catch (JWTVerificationException exception) {
            return Optional.empty();
        }
    }

    private DecodedJWT verify(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
                .withIssuer("physio-manager")
                .build()
                .verify(token);
    }

    private Instant genExpirationDate() {
        // Instant e absoluto: independe do fuso do servidor (Railway roda em UTC)
        return Instant.now().plus(VALIDADE_TOKEN);
    }
}

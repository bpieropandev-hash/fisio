package com.physio.application.service;

import com.physio.infrastructure.out.email.EmailService;
import com.physio.infrastructure.out.persistence.entity.PasswordResetTokenEntity;
import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import com.physio.infrastructure.out.persistence.repository.PasswordResetTokenJpaRepository;
import com.physio.infrastructure.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsqueciSenhaService {

    private static final Duration VALIDADE_TOKEN = Duration.ofMinutes(30);

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final PasswordResetTokenJpaRepository tokenJpaRepository;
    private final EmailService emailService;
    private final EsqueciSenhaRateLimiter rateLimiter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Nunca revela se o login/email existe: chamador sempre responde 200,
     * independente do resultado interno.
     */
    @Transactional
    public void solicitar(String loginOuEmail) {
        if (loginOuEmail == null || loginOuEmail.isBlank()) {
            return;
        }

        if (!rateLimiter.permitir(loginOuEmail.trim().toLowerCase())) {
            log.warn("Rate limit de esqueci-senha atingido para o identificador informado");
            return;
        }

        UsuarioEntity usuario = usuarioJpaRepository.findByLoginOrEmail(loginOuEmail, loginOuEmail).orElse(null);
        if (usuario == null || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            log.info("Solicitação de esqueci-senha para identificador sem conta/email associado");
            return;
        }

        String tokenBruto = UUID.randomUUID().toString();
        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .usuarioId(usuario.getId())
                .tokenHash(PasswordResetTokenHasher.hash(tokenBruto))
                .expiraEm(LocalDateTime.now().plus(VALIDADE_TOKEN))
                .usado(false)
                .criadoEm(LocalDateTime.now())
                .build();
        tokenJpaRepository.save(resetToken);

        String link = frontendUrl + "/redefinir-senha?token=" + tokenBruto;
        emailService.enviarEmailRedefinicaoSenha(usuario.getEmail(), link);

        log.info("Token de redefinição de senha gerado para usuário ID: {}", usuario.getId());
    }
}

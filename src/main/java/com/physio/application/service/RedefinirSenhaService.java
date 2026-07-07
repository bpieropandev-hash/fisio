package com.physio.application.service;

import com.physio.infrastructure.out.persistence.entity.PasswordResetTokenEntity;
import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import com.physio.infrastructure.out.persistence.repository.PasswordResetTokenJpaRepository;
import com.physio.infrastructure.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedefinirSenhaService {

    private static final String MENSAGEM_TOKEN_INVALIDO = "Token inválido ou expirado";

    private final PasswordResetTokenJpaRepository tokenJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void redefinir(String tokenBruto, String novaSenha) {
        String tokenHash = PasswordResetTokenHasher.hash(tokenBruto);

        PasswordResetTokenEntity resetToken = tokenJpaRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(MENSAGEM_TOKEN_INVALIDO));

        if (resetToken.isUsado() || resetToken.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(MENSAGEM_TOKEN_INVALIDO);
        }

        UsuarioEntity usuario = usuarioJpaRepository.findById(resetToken.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException(MENSAGEM_TOKEN_INVALIDO));

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioJpaRepository.save(usuario);

        resetToken.setUsado(true);
        tokenJpaRepository.save(resetToken);

        log.info("Senha redefinida com sucesso para usuário ID: {}", usuario.getId());
    }
}

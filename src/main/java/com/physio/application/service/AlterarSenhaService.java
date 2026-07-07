package com.physio.application.service;

import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import com.physio.infrastructure.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlterarSenhaService {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void alterar(UsuarioEntity usuarioAutenticado, String senhaAtual, String novaSenha) {
        if (!passwordEncoder.matches(senhaAtual, usuarioAutenticado.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }

        usuarioAutenticado.setSenha(passwordEncoder.encode(novaSenha));
        usuarioJpaRepository.save(usuarioAutenticado);

        log.info("Senha alterada com sucesso para usuário ID: {}", usuarioAutenticado.getId());
    }
}

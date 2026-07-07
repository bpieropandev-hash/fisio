package com.physio.application.service;

import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import com.physio.infrastructure.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtualizarFotoUsuarioService {

    private final UsuarioJpaRepository usuarioJpaRepository;

    @Transactional
    public UsuarioEntity atualizarFoto(UsuarioEntity usuarioAutenticado, String fotoBase64) {
        usuarioAutenticado.setFotoBase64(fotoBase64);

        UsuarioEntity salvo = usuarioJpaRepository.save(usuarioAutenticado);
        log.info("Foto de perfil atualizada para usuário ID: {}", salvo.getId());
        return salvo;
    }
}

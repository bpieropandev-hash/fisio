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
public class AtualizarPerfilUsuarioService {

    private final UsuarioJpaRepository usuarioJpaRepository;

    @Transactional
    public UsuarioEntity atualizar(UsuarioEntity usuarioAutenticado, String login, String nome, String telefone,
                                    String email, String corPrimaria, String fonteTema, String tamanhoFonte) {
        if (usuarioJpaRepository.existsByLoginAndIdNot(login, usuarioAutenticado.getId())) {
            throw new IllegalArgumentException("Login já está em uso");
        }

        usuarioAutenticado.setLogin(login);
        usuarioAutenticado.setNome(nome);
        usuarioAutenticado.setTelefone(telefone);
        usuarioAutenticado.setEmail(email);
        usuarioAutenticado.setCorPrimaria(corPrimaria);
        usuarioAutenticado.setFonteTema(fonteTema);
        usuarioAutenticado.setTamanhoFonte(tamanhoFonte);

        UsuarioEntity salvo = usuarioJpaRepository.save(usuarioAutenticado);
        log.info("Perfil atualizado para usuário ID: {}", salvo.getId());
        return salvo;
    }
}

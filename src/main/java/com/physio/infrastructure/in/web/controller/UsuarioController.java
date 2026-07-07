package com.physio.infrastructure.in.web.controller;

import com.physio.application.service.AtualizarFotoUsuarioService;
import com.physio.application.service.AtualizarPerfilUsuarioService;
import com.physio.infrastructure.in.web.dto.AtualizarUsuarioRequestDTO;
import com.physio.infrastructure.in.web.dto.UploadFotoRequestDTO;
import com.physio.infrastructure.in.web.dto.UsuarioMeResponseDTO;
import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários", description = "Perfil do usuário autenticado")
@RequiredArgsConstructor
public class UsuarioController {

    private final AtualizarPerfilUsuarioService atualizarPerfilUsuarioService;
    private final AtualizarFotoUsuarioService atualizarFotoUsuarioService;

    @Operation(summary = "Dados do usuário logado")
    @GetMapping("/me")
    public ResponseEntity<UsuarioMeResponseDTO> me(@AuthenticationPrincipal UsuarioEntity usuario) {
        return ResponseEntity.ok(toDTO(usuario));
    }

    @Operation(summary = "Atualizar perfil", description = "Atualiza nome, telefone e email do usuário logado")
    @PutMapping("/me")
    public ResponseEntity<UsuarioMeResponseDTO> atualizarMe(@AuthenticationPrincipal UsuarioEntity usuario,
                                                             @RequestBody @Valid AtualizarUsuarioRequestDTO data) {
        UsuarioEntity atualizado = atualizarPerfilUsuarioService.atualizar(usuario, data.login(), data.nome(),
                data.telefone(), data.email(), data.corPrimaria(), data.fonteTema(), data.tamanhoFonte());
        return ResponseEntity.ok(toDTO(atualizado));
    }

    @Operation(summary = "Atualizar foto de perfil", description = "Recebe a foto como base64 (data URI)")
    @PutMapping("/me/foto")
    public ResponseEntity<UsuarioMeResponseDTO> atualizarFoto(@AuthenticationPrincipal UsuarioEntity usuario,
                                                               @RequestBody @Valid UploadFotoRequestDTO data) {
        UsuarioEntity atualizado = atualizarFotoUsuarioService.atualizarFoto(usuario, data.fotoBase64());
        return ResponseEntity.ok(toDTO(atualizado));
    }

    private UsuarioMeResponseDTO toDTO(UsuarioEntity usuario) {
        return new UsuarioMeResponseDTO(
                usuario.getId(),
                usuario.getLogin(),
                usuario.getEmail(),
                usuario.getNome(),
                usuario.getTelefone(),
                usuario.getFotoBase64(),
                usuario.getRole(),
                usuario.getCorPrimaria(),
                usuario.getFonteTema(),
                usuario.getTamanhoFonte()
        );
    }
}

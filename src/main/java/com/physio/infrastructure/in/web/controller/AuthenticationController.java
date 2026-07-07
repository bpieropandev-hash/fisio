package com.physio.infrastructure.in.web.controller;

import com.physio.application.service.AlterarSenhaService;
import com.physio.application.service.EsqueciSenhaService;
import com.physio.application.service.RedefinirSenhaService;
import com.physio.infrastructure.in.security.TokenService;
import com.physio.infrastructure.in.web.dto.AlterarSenhaRequestDTO;
import com.physio.infrastructure.in.web.dto.AuthenticationDTO;
import com.physio.infrastructure.in.web.dto.EsqueciSenhaRequestDTO;
import com.physio.infrastructure.in.web.dto.LoginResponseDTO;
import com.physio.infrastructure.in.web.dto.RedefinirSenhaRequestDTO;
import com.physio.infrastructure.out.persistence.entity.UsuarioEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login e gestão de acesso")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final EsqueciSenhaService esqueciSenhaService;
    private final RedefinirSenhaService redefinirSenhaService;
    private final AlterarSenhaService alterarSenhaService;

    @Operation(summary = "Realizar Login", description = "Retorna um Bearer Token para ser usado nas outras requisições")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.senha());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((UsuarioEntity) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @Operation(summary = "Esqueci minha senha",
            description = "Envia email com link de redefinição, se a conta existir. Sempre responde 200 para não revelar se o login/email existe.")
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@RequestBody @Valid EsqueciSenhaRequestDTO data) {
        esqueciSenhaService.solicitar(data.loginOuEmail());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Redefinir senha", description = "Define uma nova senha a partir do token recebido por email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")
    })
    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@RequestBody @Valid RedefinirSenhaRequestDTO data) {
        redefinirSenhaService.redefinir(data.token(), data.novaSenha());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Alterar senha", description = "Altera a senha do usuário autenticado (senha atual + nova)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta")
    })
    @PutMapping("/alterar-senha")
    public ResponseEntity<Void> alterarSenha(@AuthenticationPrincipal UsuarioEntity usuario,
                                              @RequestBody @Valid AlterarSenhaRequestDTO data) {
        alterarSenhaService.alterar(usuario, data.senhaAtual(), data.novaSenha());
        return ResponseEntity.ok().build();
    }
}
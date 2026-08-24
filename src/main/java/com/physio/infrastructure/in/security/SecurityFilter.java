package com.physio.infrastructure.in.security;

import com.physio.infrastructure.out.persistence.repository.UsuarioJpaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Adicionado para logs
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j // Anotação do Lombok para logs
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioJpaRepository usuarioRepository;

    private static final String HEADER_TOKEN_RENOVADO = "X-Renewed-Token";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);

        if (token != null) {
            // Nunca logar o token: e uma credencial valida por 8h
            var loginOpt = tokenService.validateToken(token);

            if (loginOpt.isPresent()) {
                var login = loginOpt.get();
                UserDetails user = usuarioRepository.findByLogin(login);

                if (user != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Sliding expiration (P3.3): perto de expirar, renova e devolve no header
                    // pro front trocar o token guardado sem derrubar a sessão no meio do uso.
                    tokenService.renovarSeProximoDaExpiracao(token)
                            .ifPresent(novoToken -> response.setHeader(HEADER_TOKEN_RENOVADO, novoToken));
                } else {
                    log.warn("Usuário não encontrado no banco para o login: {}", login);
                }
            } else {
                log.debug("Token inválido ou expirado.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        // Remove o prefixo Bearer se existir, e remove espaços em branco extras
        return authHeader.replace("Bearer ", "").trim();
    }
}
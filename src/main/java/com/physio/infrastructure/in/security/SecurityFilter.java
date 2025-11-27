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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);

        if (token != null) {
            log.info("Token recebido no filtro: {}", token); // LOG 1

            var login = tokenService.validateToken(token);
            log.info("Login extraído do token: {}", login); // LOG 2

            if (!login.isEmpty()) {
                UserDetails user = usuarioRepository.findByLogin(login);

                if (user != null) {
                    log.info("Usuário encontrado no banco: {}", user.getUsername()); // LOG 3
                    log.info("Permissões do usuário: {}", user.getAuthorities()); // LOG 4

                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("Usuário autenticado no contexto de segurança com sucesso.");
                } else {
                    log.warn("Usuário não encontrado no banco para o login: {}", login);
                }
            } else {
                log.warn("Token inválido ou expirado (login vazio).");
            }
        } else {
            log.debug("Nenhum token encontrado na requisição.");
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
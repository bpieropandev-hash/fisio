package com.physio.infrastructure.in.web.exception;

import com.physio.infrastructure.in.web.dto.ErrorResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFound(EntityNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND,
                ex.getMessage() != null ? ex.getMessage() : "Recurso não encontrado");
    }

    @ExceptionHandler({UsernameNotFoundException.class, InternalAuthenticationServiceException.class})
    public ResponseEntity<ErrorResponseDTO> handleUserNotFound(RuntimeException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(AuthenticationException ex) {
        // Não expor detalhes ao cliente; logar para diagnóstico
        log.warn("Authentication failure: {}", ex.getMessage());
        return body(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Requisição inválida");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return body(HttpStatus.BAD_REQUEST, "Erro de validação", erros);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> erros = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .toList();
        return body(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", erros);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Nunca repassar a mensagem do banco (expõe nomes de tabelas/constraints)
        log.warn("Violação de integridade de dados", ex);
        return body(HttpStatus.CONFLICT,
                "Conflito de dados: já existe um registro com essas informações ou a operação viola um vínculo existente.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFound(NoResourceFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "Rota não encontrada: " + ex.getResourcePath());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotReadable(HttpMessageNotReadableException ex) {
        // Corpo malformado (JSON inválido, encoding errado, data fora do padrão) é erro do cliente
        log.warn("Corpo de requisição ilegível: {}", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN,
                ex.getMessage() != null ? ex.getMessage() : "Acesso negado");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAll(Exception ex) {
        // Log completo no servidor; nada de detalhes internos para o cliente
        log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Contate o suporte.");
    }

    private ResponseEntity<ErrorResponseDTO> body(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErrorResponseDTO(status.value(), mensagem));
    }

    private ResponseEntity<ErrorResponseDTO> body(HttpStatus status, String mensagem, List<String> erros) {
        return ResponseEntity.status(status).body(new ErrorResponseDTO(status.value(), mensagem, erros));
    }
}

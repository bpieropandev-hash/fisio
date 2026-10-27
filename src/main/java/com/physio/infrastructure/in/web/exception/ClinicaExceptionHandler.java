package com.physio.infrastructure.in.web.exception;
import com.physio.infrastructure.in.web.controller.*;
import com.physio.application.service.EvolucaoVersaoConflitanteException;
import com.physio.infrastructure.in.web.dto.ErrorResponseDTO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes={FotoClinicaController.class,EvolucaoClinicaController.class})
public class ClinicaExceptionHandler {
    @ExceptionHandler(EvolucaoVersaoConflitanteException.class)
    public ResponseEntity<ErrorResponseDTO> conflito(EvolucaoVersaoConflitanteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(409, ex.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponseDTO> requisicaoInvalida(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponseDTO(400,
                "Verifique os campos obrigatórios, identificadores e o formato das datas."));
    }
}

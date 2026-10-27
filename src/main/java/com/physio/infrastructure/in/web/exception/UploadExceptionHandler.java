package com.physio.infrastructure.in.web.exception;

import com.physio.infrastructure.in.web.dto.ErrorResponseDTO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// O limite multipart pode falhar antes da seleção de um controller; este advice não é restrito por tipo.
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class UploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> limiteDeUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponseDTO(400,
                "Envie uma imagem JPEG ou PNG de até 5 MiB."));
    }
}

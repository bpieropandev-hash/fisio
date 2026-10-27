package com.physio.infrastructure.in.web.controller;
import com.physio.application.service.FotoClinicaService;
import com.physio.infrastructure.in.web.dto.FotoClinicaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/pacientes/{pacienteId}/fotos")
public class FotoClinicaController {
    private final FotoClinicaService service;

    @GetMapping
    public List<FotoClinicaDTO> listar(@PathVariable Integer pacienteId) {
        return service.listar(pacienteId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FotoClinicaDTO criar(@PathVariable Integer pacienteId,
            @RequestParam MultipartFile imagem,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaptura,
            @RequestParam String regiao, @RequestParam(defaultValue = "") String observacao,
            Principal principal) {
        return service.criar(pacienteId, imagem, dataCaptura, regiao, observacao, principal.getName());
    }

    @GetMapping("/{fotoId}/imagem")
    public ResponseEntity<byte[]> imagem(@PathVariable Integer pacienteId, @PathVariable Long fotoId) {
        var imagem = service.imagem(pacienteId, fotoId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagem.tipoConteudo()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff").body(imagem.conteudo());
    }

    @DeleteMapping("/{fotoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer pacienteId, @PathVariable Long fotoId) {
        service.excluir(pacienteId, fotoId);
    }
}

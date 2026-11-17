package com.physio.infrastructure.in.web.controller;
import com.physio.application.service.AvaliacaoFisicaService;
import com.physio.infrastructure.in.web.dto.AvaliacaoFisicaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/pacientes/{pacienteId}/avaliacoes")
public class AvaliacaoFisicaController {
    private final AvaliacaoFisicaService service;

    @GetMapping
    public List<AvaliacaoFisicaDTO> listar(@PathVariable Integer pacienteId) {
        return service.listar(pacienteId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoFisicaDTO criar(@PathVariable Integer pacienteId,
            @RequestParam MultipartFile arquivo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAvaliacao,
            @RequestParam String titulo, @RequestParam(defaultValue = "") String observacao,
            Principal principal) {
        return service.criar(pacienteId, arquivo, dataAvaliacao, titulo, observacao, principal.getName());
    }

    @GetMapping("/{avaliacaoId}/arquivo")
    public ResponseEntity<byte[]> arquivo(@PathVariable Integer pacienteId, @PathVariable Long avaliacaoId) {
        var a = service.arquivo(pacienteId, avaliacaoId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.tipoConteudo()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff").body(a.conteudo());
    }

    @DeleteMapping("/{avaliacaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer pacienteId, @PathVariable Long avaliacaoId) {
        service.excluir(pacienteId, avaliacaoId);
    }
}

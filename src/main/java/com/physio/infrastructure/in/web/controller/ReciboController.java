package com.physio.infrastructure.in.web.controller;
import com.physio.application.service.ReciboService;
import com.physio.infrastructure.in.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/recibos") @RequiredArgsConstructor
public class ReciboController {
 private final ReciboService service;
 @PostMapping public ResponseEntity<ReciboResponseDTO> emitir(@Valid @RequestBody ReciboRequestDTO request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.emitir(request)); }
 @GetMapping public List<ReciboResponseDTO> listar(@RequestParam(required=false) Integer atendimentoId,@RequestParam(required=false) Integer cobrancaId) { return service.listar(atendimentoId,cobrancaId); }
 @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE) public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=recibo-"+id+".pdf")
   .cacheControl(CacheControl.noStore()).body(service.pdf(id));
 }
}

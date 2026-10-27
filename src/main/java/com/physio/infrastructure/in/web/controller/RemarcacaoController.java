package com.physio.infrastructure.in.web.controller;

import com.physio.application.service.RemarcacaoService;
import static com.physio.application.service.RemarcacaoContrato.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.security.Principal;
import java.util.List;

@RestController @RequestMapping("/api/v1/remarcacoes") @RequiredArgsConstructor
public class RemarcacaoController {
 private final RemarcacaoService service;
 @GetMapping("/candidatos") public List<Sessao> candidatos(@RequestParam Long pacienteId,@RequestParam Long servicoId,
  @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim){return service.candidatos(pacienteId,servicoId,inicio,fim);}
 @PostMapping("/previa") public Previa prever(@RequestBody Pedido pedido,Principal principal){return service.prever(pedido,principal.getName());}
 @PostMapping("/{id}/confirmar") public Resultado confirmar(@PathVariable String id,Principal principal){return service.confirmar(id,principal.getName());}
}

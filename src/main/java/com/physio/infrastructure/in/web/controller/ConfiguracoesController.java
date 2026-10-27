package com.physio.infrastructure.in.web.controller;

import com.physio.application.service.ConfiguracoesService;
import com.physio.infrastructure.in.web.dto.ConfiguracoesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configuracoes")
@RequiredArgsConstructor
public class ConfiguracoesController {
    private final ConfiguracoesService service;
    @GetMapping public ConfiguracoesDTO buscar() { return service.buscar(); }
    @PutMapping public ConfiguracoesDTO salvar(@RequestBody ConfiguracoesDTO request) { return service.salvar(request); }
}

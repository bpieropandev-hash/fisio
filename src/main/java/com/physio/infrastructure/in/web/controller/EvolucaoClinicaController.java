package com.physio.infrastructure.in.web.controller;
import com.physio.application.service.EvolucaoClinicaService;
import com.physio.infrastructure.in.web.dto.EvolucaoClinicaDTO;
import com.physio.infrastructure.in.web.dto.EvolucaoClinicaRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/agendamentos/{id}/evolucao-estruturada")
public class EvolucaoClinicaController {
    private final EvolucaoClinicaService service;

    @GetMapping
    public EvolucaoClinicaDTO buscar(@PathVariable Integer id) { return service.buscar(id); }

    @GetMapping("/historico")
    public List<EvolucaoClinicaDTO> historico(@PathVariable Integer id) { return service.historico(id); }

    @PutMapping
    public EvolucaoClinicaDTO salvar(@PathVariable Integer id, @RequestBody EvolucaoClinicaRequestDTO request,
                                    Principal principal) {
        return service.salvar(id, request, principal.getName());
    }
}

package com.physio.application.service;
import com.physio.domain.model.*;
import com.physio.domain.ports.out.AtendimentoRepositoryPort;
import java.time.LocalDateTime;
import java.util.Objects;
public final class ValidadorCapacidade {
 private ValidadorCapacidade() {}
 public static int capacidade(ServicoConfig servico) { return servico.getCapacidade()!=null ? servico.getCapacidade() : servico.getTipo()==TipoServico.PILATES?15:1; }
 public static void validar(AtendimentoRepositoryPort repo, ServicoConfig servico, Integer pacienteId, LocalDateTime inicio, LocalDateTime fim, Integer ignorar) {
  if(inicio==null||fim==null||!fim.isAfter(inicio)) throw new IllegalArgumentException("Fim deve ser posterior ao início");
  var conflitos=repo.listarConflitosPorPeriodo(inicio,fim).stream().filter(a->a.getStatus()!=StatusAtendimento.CANCELADO && (ignorar==null||!Objects.equals(a.getId(),ignorar))).toList();
  if(conflitos.stream().anyMatch(a->a.getPaciente()!=null && Objects.equals(a.getPaciente().getId(),pacienteId))) throw new IllegalArgumentException("Paciente já possui atendimento neste horário");
  if(conflitos.stream().anyMatch(a->!Objects.equals(a.getServicoBase().getId(),servico.getId()))) throw new IllegalArgumentException("Serviço diferente de outros agendamentos no mesmo horário");
  if(conflitos.size()+1>capacidade(servico)) throw new IllegalArgumentException("Capacidade máxima neste horário atingida ("+capacidade(servico)+" pacientes)");
 }
}

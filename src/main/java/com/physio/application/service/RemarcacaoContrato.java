package com.physio.application.service;

import java.time.*;
import java.math.BigDecimal;
import java.util.List;

public final class RemarcacaoContrato {
 private RemarcacaoContrato() {}
 public enum Modo { MANTER_DATAS, SUBSTITUIR_DIAS }
 public record Pedido(Long pacienteId, Long servicoId, List<Integer> origemIds, LocalDate inicio,
                      LocalDate fim, Modo modo, LocalTime horario, List<Integer> diasSemana, String motivo) {}
 public record Sessao(Integer id, LocalDateTime inicio, LocalDateTime fim, String protecao) {}
 public record Destino(Integer origemId, LocalDateTime inicio, LocalDateTime fim, String modalidade, BigDecimal valor) {}
 public record Conflito(LocalDateTime inicio, String motivo) {}
 public record Previa(String operacaoId, List<Sessao> origens, List<Destino> destinos,
                      List<Conflito> conflitos, int elegiveis, int protegidos, int mantidos, int cancelados,
                      int criados, int adicionais, int frequenciaAnterior, int frequenciaNova,
                      int novasAvulsas, BigDecimal valorNovasAvulsas) {}
 public record Resultado(String operacaoId, List<Integer> origemIds, List<Integer> destinoIds) {}
}

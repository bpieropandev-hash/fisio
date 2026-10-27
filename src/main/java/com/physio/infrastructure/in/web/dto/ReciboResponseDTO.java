package com.physio.infrastructure.in.web.dto;
import java.math.BigDecimal;
import java.time.*;
public record ReciboResponseDTO(Long id,String numero,Integer atendimentoId,Integer cobrancaId,String pacienteNome,String pacienteDocumento,
 String pagadorNome,String pagadorDocumento,String emitenteNome,String emitenteDocumento,String descricao,BigDecimal valor,
 LocalDate dataPagamento,String tipoPagamento,LocalDateTime emitidoEm) {}

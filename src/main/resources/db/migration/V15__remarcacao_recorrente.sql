CREATE TABLE remarcacao_operacoes (
 id varchar(36) PRIMARY KEY,
 autor varchar(255) NOT NULL,
 motivo varchar(1000) NOT NULL,
 criada_em timestamp NOT NULL,
 confirmada_em timestamp,
 pedido text NOT NULL,
 assinatura text NOT NULL,
 previa text NOT NULL,
 resultado text
);
COMMENT ON TABLE remarcacao_operacoes IS 'Seleção explícita de origens legadas e destinos; auditoria de cancelamento por remarcação, sem inferir séries.';

-- Migração: Criação das tabelas de Assinaturas e Cobranças Mensais
-- V2: Suporte a pagamentos recorrentes (mensalidades)

-- Tabela de Assinaturas
CREATE TABLE IF NOT EXISTS assinaturas (
    id SERIAL PRIMARY KEY,
    paciente_id INTEGER NOT NULL,
    servico_id INTEGER NOT NULL,
    valor_mensal DECIMAL(10, 2) NOT NULL,
    dia_vencimento INTEGER NOT NULL CHECK (dia_vencimento >= 1 AND dia_vencimento <= 28),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_inicio DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE,
    FOREIGN KEY (servico_id) REFERENCES servicos_config(id) ON DELETE CASCADE
);

-- Índice único parcial para garantir apenas uma assinatura ativa por paciente/serviço
CREATE UNIQUE INDEX IF NOT EXISTS idx_assinatura_ativa_unica 
    ON assinaturas(paciente_id, servico_id) 
    WHERE ativo = TRUE;

-- Índices para melhor performance
CREATE INDEX IF NOT EXISTS idx_assinaturas_paciente ON assinaturas(paciente_id);
CREATE INDEX IF NOT EXISTS idx_assinaturas_servico ON assinaturas(servico_id);
CREATE INDEX IF NOT EXISTS idx_assinaturas_ativo ON assinaturas(ativo);

-- Tabela de Cobranças Mensais
CREATE TABLE IF NOT EXISTS cobrancas_mensais (
    id SERIAL PRIMARY KEY,
    assinatura_id INTEGER NOT NULL,
    mes_referencia INTEGER NOT NULL CHECK (mes_referencia >= 1 AND mes_referencia <= 12),
    ano_referencia INTEGER NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PAGO')),
    data_pagamento DATE,
    recebedor VARCHAR(10) CHECK (recebedor IN ('CLINICA', 'PROFISSIONAL')),
    tipo_pagamento VARCHAR(25) CHECK (tipo_pagamento IN ('DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX')),
    pct_clinica_snapshot DECIMAL(5, 2) NOT NULL,
    pct_profissional_snapshot DECIMAL(5, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assinatura_id) REFERENCES assinaturas(id) ON DELETE CASCADE,
    CONSTRAINT uq_cobranca_mes_ano UNIQUE (assinatura_id, mes_referencia, ano_referencia)
);

-- Índices para melhor performance
CREATE INDEX IF NOT EXISTS idx_cobrancas_assinatura ON cobrancas_mensais(assinatura_id);
CREATE INDEX IF NOT EXISTS idx_cobrancas_status ON cobrancas_mensais(status);
CREATE INDEX IF NOT EXISTS idx_cobrancas_mes_ano ON cobrancas_mensais(ano_referencia, mes_referencia);
CREATE INDEX IF NOT EXISTS idx_cobrancas_recebedor ON cobrancas_mensais(recebedor) WHERE recebedor IS NOT NULL;

-- Comentários para documentação
COMMENT ON TABLE assinaturas IS 'Armazena assinaturas mensais de pacientes para serviços específicos (ex: Pilates)';
COMMENT ON TABLE cobrancas_mensais IS 'Armazena as cobranças mensais geradas a partir das assinaturas ativas';


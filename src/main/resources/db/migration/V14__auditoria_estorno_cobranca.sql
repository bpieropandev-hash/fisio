CREATE TABLE estornos_cobranca (
    id BIGSERIAL PRIMARY KEY,
    cobranca_id INTEGER NOT NULL REFERENCES cobrancas_mensais(id),
    motivo VARCHAR(1000) NOT NULL,
    autor VARCHAR(255) NOT NULL,
    estornado_em TIMESTAMP NOT NULL,
    data_pagamento_anterior DATE,
    recebedor_anterior VARCHAR(25),
    tipo_pagamento_anterior VARCHAR(25)
);
CREATE INDEX idx_estornos_cobranca_origem ON estornos_cobranca(cobranca_id);

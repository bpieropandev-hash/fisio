CREATE TABLE configuracoes_clinica (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    versao BIGINT NOT NULL DEFAULT 0 CHECK (versao >= 0),
    configuracao TEXT
);
-- NULL distingue a instalação ainda não configurada (fallback ENV) de campos salvos vazios.
INSERT INTO configuracoes_clinica (id, versao, configuracao) VALUES (1, 0, NULL);
-- Até 50 códigos de até 50 caracteres, preservando todas as revisões existentes.
ALTER TABLE evolucoes_clinicas ALTER COLUMN procedimentos TYPE VARCHAR(2550);
ALTER TABLE evolucoes_clinicas ALTER COLUMN equipamentos TYPE VARCHAR(2550);

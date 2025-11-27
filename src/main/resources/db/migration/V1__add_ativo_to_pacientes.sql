-- Migração: adiciona coluna 'ativo' à tabela pacientes
-- Adiciona a coluna com default TRUE e atualiza linhas existentes para TRUE

ALTER TABLE pacientes
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN DEFAULT TRUE;

-- Garante que todas as linhas existentes tenham valor (em caso de versões antigas do Postgres que não setam o default retroativamente)
UPDATE pacientes SET ativo = TRUE WHERE ativo IS NULL;

-- Opcional: adicionar índice se for comum filtrar por ativo
-- CREATE INDEX IF NOT EXISTS idx_pacientes_ativo ON pacientes(ativo);


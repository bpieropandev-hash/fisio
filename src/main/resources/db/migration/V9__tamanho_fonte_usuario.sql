-- V9: Tamanho de fonte configurável por usuário (spec 09) - pequeno/medio/grande.
-- NULL = usa o default do sistema ("medio").
-- Idempotente: pode ser executada em produção.

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS tamanho_fonte VARCHAR(10);

-- V7: Perfil de usuário (spec 08 Feature B) - nome, telefone, foto.
-- Foto guardada como base64 direto na coluna (Opção 3 da spec: zero infra nova,
-- aceitável para 1 foto por usuário e poucos usuários).
-- Idempotente: pode ser executada em produção.

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS nome VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS telefone VARCHAR(20);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS foto_base64 TEXT;

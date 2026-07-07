-- V8: Tema personalizado por usuário (spec 09) - cor de acento + par de fonte.
-- NULL em qualquer uma das duas = usa o default do sistema (cor #4cc9f0, fonte "moderno").
-- Idempotente: pode ser executada em produção.

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS cor_primaria VARCHAR(7);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fonte_tema VARCHAR(20);

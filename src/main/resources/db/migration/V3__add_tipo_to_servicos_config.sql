-- Add tipo column to servicos_config to indicate service type (FISIOTERAPIA or PILATES)
ALTER TABLE servicos_config
ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'FISIOTERAPIA';

-- Optional: update existing rows if you want to set some to PILATES manually
-- UPDATE servicos_config SET tipo = 'PILATES' WHERE nome ILIKE '%pilates%';


-- Add status_pagamento column to atendimentos table
-- This column tracks payment status: PAID or PAYMENT_PENDING
ALTER TABLE atendimentos
ADD COLUMN IF NOT EXISTS status_pagamento VARCHAR(20) NOT NULL DEFAULT 'PAYMENT_PENDING';

-- Update existing records: if recebedor and tipo_pagamento are both set, mark as PAID
UPDATE atendimentos
SET status_pagamento = 'PAID'
WHERE recebedor IS NOT NULL AND tipo_pagamento IS NOT NULL;

-- For records without payment data, ensure they are marked as PAYMENT_PENDING
UPDATE atendimentos
SET status_pagamento = 'PAYMENT_PENDING'
WHERE recebedor IS NULL OR tipo_pagamento IS NULL;

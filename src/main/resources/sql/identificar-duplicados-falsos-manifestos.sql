-- ================================================================
-- Script: identificar-duplicados-falsos-manifestos.sql
-- Finalidade: Identificar duplicados falsos no banco atual
--             (manifestos com mesmo sequence_code mas identificador_unico diferente
--              devido a campos voláteis no hash)
-- 
-- ⚠️ ATENÇÃO: Execute este script ANTES de aplicar a correção
--             para entender o impacto da migração
-- ================================================================

-- Identificar duplicados falsos no banco atual
WITH registros_metadata AS (
    SELECT 
        id,
        sequence_code,
        identificador_unico,
        pick_sequence_code,
        -- Campos estáveis para novo hash
        created_at,
        status,
        vehicle_plate,
        driver_name,
        classification,
        data_extracao
    FROM manifestos
    WHERE pick_sequence_code IS NULL
)
SELECT 
    sequence_code,
    COUNT(*) as total_registros,
    STRING_AGG(CAST(id AS VARCHAR), ', ') as ids,
    STRING_AGG(identificador_unico, ', ') as identificadores_unicos,
    MIN(data_extracao) as primeira_extracao,
    MAX(data_extracao) as ultima_extracao
FROM registros_metadata
GROUP BY sequence_code
HAVING COUNT(*) > 1
ORDER BY COUNT(*) DESC;

-- ================================================================
-- Resultado Esperado:
-- 
-- Este script deve retornar os manifestos que são duplicados falsos,
-- ou seja, têm o mesmo sequence_code mas identificador_unico diferente
-- devido a campos voláteis (mobile_read_at, departured_at, etc.)
-- que mudaram entre extrações.
-- 
-- Após aplicar a correção no código e executar nova extração,
-- estes duplicados devem ser resolvidos automaticamente.
-- ================================================================


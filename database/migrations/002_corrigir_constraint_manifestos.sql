-- ============================================================================
-- CORREÇÃO CRÍTICA #4: Alinhar Constraint UNIQUE com Lógica de MERGE
-- ============================================================================
-- Arquivo: 002_corrigir_constraint_manifestos.sql
-- Descrição: Corrige inconsistência entre constraint UNIQUE e lógica de MERGE
-- Data: 04/02/2026
-- Autor: Sistema de Auditoria
-- 
-- PROBLEMA ORIGINAL:
-- - MERGE usa: (sequence_code, pick_sequence_code, mdfe_number)
-- - Constraint UNIQUE usa: (sequence_code, identificador_unico)
-- 
-- SOLUÇÃO:
-- - Alinhar constraint para usar a mesma chave composta do MERGE
-- - Isso permite múltiplos MDF-es e coletas para o mesmo sequence_code
-- ============================================================================

USE [ESL_Cloud_ETL];
GO

PRINT '🔧 Iniciando correção de constraint em MANIFESTOS...';
PRINT '';

-- ============================================================================
-- PASSO 1: Backup da Constraint Atual
-- ============================================================================

PRINT '📋 PASSO 1: Verificando constraint atual...';

IF EXISTS (
    SELECT 1 FROM sys.key_constraints 
    WHERE name = 'UQ_manifestos_sequence_identificador' 
    AND parent_object_id = OBJECT_ID('dbo.manifestos')
)
BEGIN
    PRINT '  ℹ️  Constraint antiga encontrada: UQ_manifestos_sequence_identificador';
    PRINT '      (sequence_code, identificador_unico)';
    
    -- Verificar duplicados que seriam permitidos com nova constraint
    PRINT '';
    PRINT '  🔍 Verificando registros que se tornarão válidos...';
    
    SELECT 
        sequence_code,
        COUNT(*) as total_registros,
        COUNT(DISTINCT pick_sequence_code) as picks_distintos,
        COUNT(DISTINCT mdfe_number) as mdfes_distintos
    FROM manifestos
    GROUP BY sequence_code
    HAVING COUNT(*) > 1;
    
    IF @@ROWCOUNT > 0
    BEGIN
        PRINT '  ⚠️  ATENÇÃO: Existem manifestos com múltiplos picks/MDFes (correto!)';
        PRINT '      Estes registros são duplicados NATURAIS e devem ser preservados.';
    END
    ELSE
    BEGIN
        PRINT '  ✅ Nenhum duplicado natural encontrado.';
    END
END
ELSE
BEGIN
    PRINT '  ⚠️  Constraint UQ_manifestos_sequence_identificador NÃO encontrada.';
    PRINT '      Tabela pode estar usando estrutura antiga.';
END

PRINT '';

-- ============================================================================
-- PASSO 2: Verificar Duplicados que Violariam Nova Constraint
-- ============================================================================

PRINT '📋 PASSO 2: Verificando duplicados que violariam nova constraint...';
PRINT '  (Mesma chave composta: sequence_code, pick_sequence_code, mdfe_number)';
PRINT '';

SELECT 
    sequence_code,
    ISNULL(CAST(pick_sequence_code AS VARCHAR), 'NULL') as pick_seq,
    ISNULL(CAST(mdfe_number AS VARCHAR), 'NULL') as mdfe_num,
    COUNT(*) as total_duplicados
FROM manifestos
GROUP BY sequence_code, pick_sequence_code, mdfe_number
HAVING COUNT(*) > 1;

IF @@ROWCOUNT > 0
BEGIN
    PRINT '';
    PRINT '  ❌ ERRO: Existem duplicados que violariam a nova constraint!';
    PRINT '     Estes são duplicados FALSOS e devem ser removidos antes da migração.';
    PRINT '';
    PRINT '  💡 Ações recomendadas:';
    PRINT '     1. Execute --validar-manifestos para identificar duplicados falsos';
    PRINT '     2. Delete registros duplicados manualmente';
    PRINT '     3. Execute este script novamente';
    PRINT '';
    PRINT '  🚫 MIGRAÇÃO ABORTADA.';
    RAISERROR('Duplicados falsos encontrados. Corrija antes de continuar.', 16, 1);
    RETURN;
END
ELSE
BEGIN
    PRINT '  ✅ Nenhum duplicado falso encontrado. Seguro para migração.';
END

PRINT '';

-- ============================================================================
-- PASSO 3: Remover Constraint Antiga (se existir)
-- ============================================================================

PRINT '📋 PASSO 3: Removendo constraint antiga...';

IF EXISTS (
    SELECT 1 FROM sys.key_constraints 
    WHERE name = 'UQ_manifestos_sequence_identificador' 
    AND parent_object_id = OBJECT_ID('dbo.manifestos')
)
BEGIN
    BEGIN TRY
        ALTER TABLE dbo.manifestos 
        DROP CONSTRAINT UQ_manifestos_sequence_identificador;
        
        PRINT '  ✅ Constraint antiga removida com sucesso.';
    END TRY
    BEGIN CATCH
        PRINT '  ❌ Erro ao remover constraint antiga:';
        PRINT '     ' + ERROR_MESSAGE();
        THROW;
    END CATCH
END
ELSE
BEGIN
    PRINT '  ⏭️  Constraint antiga não existe (tabela já migrada ou estrutura antiga).';
END

PRINT '';

-- ============================================================================
-- PASSO 4: Criar Nova Constraint (Chave Composta)
-- ============================================================================

PRINT '📋 PASSO 4: Criando nova constraint alinhada com MERGE...';
PRINT '  Chave: (sequence_code, pick_sequence_code, mdfe_number)';
PRINT '';

IF NOT EXISTS (
    SELECT 1 FROM sys.key_constraints 
    WHERE name = 'UQ_manifestos_chave_composta' 
    AND parent_object_id = OBJECT_ID('dbo.manifestos')
)
BEGIN
    BEGIN TRY
        -- Criar constraint UNIQUE com a mesma chave usada no MERGE
        -- IMPORTANTE: Usa COALESCE para tratar NULLs da mesma forma que o MERGE
        -- Mas constraints UNIQUE tratam NULL de forma diferente (NULL <> NULL)
        -- Por isso, precisamos de uma coluna computada
        
        -- Opção 1: Adicionar coluna computada para chave composta
        IF NOT EXISTS (
            SELECT 1 FROM sys.columns 
            WHERE name = 'chave_merge_hash' 
            AND object_id = OBJECT_ID('dbo.manifestos')
        )
        BEGIN
            PRINT '  🔧 Adicionando coluna computada chave_merge_hash...';
            
            ALTER TABLE dbo.manifestos
            ADD chave_merge_hash AS (
                CAST(sequence_code AS VARCHAR(20)) + '|' +
                ISNULL(CAST(pick_sequence_code AS VARCHAR(20)), '-1') + '|' +
                ISNULL(CAST(mdfe_number AS VARCHAR(20)), '-1')
            ) PERSISTED;
            
            PRINT '  ✅ Coluna computada criada.';
        END
        
        -- Criar constraint UNIQUE na coluna computada
        ALTER TABLE dbo.manifestos
        ADD CONSTRAINT UQ_manifestos_chave_composta
        UNIQUE (chave_merge_hash);
        
        PRINT '  ✅ Nova constraint criada com sucesso!';
        PRINT '     Nome: UQ_manifestos_chave_composta';
        PRINT '     Chave: chave_merge_hash (sequence_code|pick|mdfe)';
        
    END TRY
    BEGIN CATCH
        PRINT '  ❌ Erro ao criar nova constraint:';
        PRINT '     ' + ERROR_MESSAGE();
        THROW;
    END CATCH
END
ELSE
BEGIN
    PRINT '  ⏭️  Nova constraint já existe (tabela já migrada).';
END

PRINT '';

-- ============================================================================
-- PASSO 5: Validação Final
-- ============================================================================

PRINT '📋 PASSO 5: Validação final...';
PRINT '';

-- Verificar que constraint existe
IF EXISTS (
    SELECT 1 FROM sys.key_constraints 
    WHERE name = 'UQ_manifestos_chave_composta' 
    AND parent_object_id = OBJECT_ID('dbo.manifestos')
)
BEGIN
    PRINT '  ✅ Constraint UQ_manifestos_chave_composta confirmada.';
    
    -- Estatísticas
    DECLARE @total INT, @distintos INT;
    
    SELECT @total = COUNT(*) FROM manifestos;
    SELECT @distintos = COUNT(DISTINCT chave_merge_hash) FROM manifestos;
    
    PRINT '  📊 Estatísticas:';
    PRINT '     Total de registros: ' + CAST(@total AS VARCHAR);
    PRINT '     Chaves únicas: ' + CAST(@distintos AS VARCHAR);
    
    IF @total = @distintos
    BEGIN
        PRINT '  ✅ Integridade confirmada: Nenhum duplicado!';
    END
    ELSE
    BEGIN
        PRINT '  ⚠️  ATENÇÃO: ' + CAST(@total - @distintos AS VARCHAR) + ' duplicados ainda existem!';
        PRINT '     Isso não deveria acontecer. Execute validação manual.';
    END
END
ELSE
BEGIN
    PRINT '  ❌ ERRO: Constraint não foi criada corretamente!';
    RAISERROR('Falha na validação da constraint.', 16, 1);
    RETURN;
END

PRINT '';
PRINT '✅ Migração concluída com sucesso!';
PRINT '';
PRINT '💡 Próximos passos:';
PRINT '   1. Execute --validar-manifestos para verificar integridade';
PRINT '   2. Execute uma extração de teste';
PRINT '   3. Monitore logs para garantir que MERGE está funcionando';
PRINT '   4. Considere REBUILD da tabela para otimizar fragmentação';
PRINT '';

GO

-- ============================================
-- Script de validação das Views de Dimensão
-- Execute este script APÓS criar todas as views de dimensão (019-023)
-- 
-- VALIDAÇÃO CRÍTICA: Verifica se as views garantem unicidade das chaves primárias
-- Se alguma view retornar chaves duplicadas, os relacionamentos no Power BI vão quebrar
-- ============================================

PRINT '============================================';
PRINT 'VALIDANDO VIEWS DE DIMENSÃO - UNICIDADE DE CHAVES';
PRINT '============================================';
PRINT '';

-- ============================================
-- Validação 1: vw_dim_clientes (Chave: Nome)
-- ============================================
PRINT '1. Validando vw_dim_clientes (Chave: Nome)...';

IF EXISTS (SELECT 1 FROM sys.views WHERE name = 'vw_dim_clientes' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DECLARE @ClientesTotal INT;
    DECLARE @ClientesDistinct INT;
    
    SELECT @ClientesTotal = COUNT(*) FROM dbo.vw_dim_clientes;
    SELECT @ClientesDistinct = COUNT(DISTINCT [Nome]) FROM dbo.vw_dim_clientes;
    
    IF @ClientesTotal = @ClientesDistinct
    BEGIN
        PRINT '   ✅ OK: vw_dim_clientes - ' + CAST(@ClientesTotal AS VARCHAR) + ' nomes únicos';
    END
    ELSE
    BEGIN
        PRINT '   ❌ ERRO: vw_dim_clientes tem nomes duplicados!';
        PRINT '   Total de linhas: ' + CAST(@ClientesTotal AS VARCHAR);
        PRINT '   Nomes únicos: ' + CAST(@ClientesDistinct AS VARCHAR);
        PRINT '   Nomes duplicados encontrados:';
        
        SELECT [Nome], COUNT(*) AS Quantidade
        FROM dbo.vw_dim_clientes
        GROUP BY [Nome]
        HAVING COUNT(*) > 1;
    END
END
ELSE
BEGIN
    PRINT '   ⚠️ AVISO: View vw_dim_clientes não existe ainda';
END
GO

-- ============================================
-- Validação 2: vw_dim_filiais (Chave: NomeFilial)
-- ============================================
PRINT '';
PRINT '2. Validando vw_dim_filiais (Chave: NomeFilial)...';

IF EXISTS (SELECT 1 FROM sys.views WHERE name = 'vw_dim_filiais' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DECLARE @FiliaisTotal INT;
    DECLARE @FiliaisDistinct INT;
    
    SELECT @FiliaisTotal = COUNT(*) FROM dbo.vw_dim_filiais;
    SELECT @FiliaisDistinct = COUNT(DISTINCT [NomeFilial]) FROM dbo.vw_dim_filiais;
    
    IF @FiliaisTotal = @FiliaisDistinct
    BEGIN
        PRINT '   ✅ OK: vw_dim_filiais - ' + CAST(@FiliaisTotal AS VARCHAR) + ' filiais únicas';
    END
    ELSE
    BEGIN
        PRINT '   ❌ ERRO: vw_dim_filiais tem nomes duplicados!';
        PRINT '   Total de linhas: ' + CAST(@FiliaisTotal AS VARCHAR);
        PRINT '   Nomes únicos: ' + CAST(@FiliaisDistinct AS VARCHAR);
        PRINT '   Nomes duplicados encontrados:';
        
        SELECT [NomeFilial], COUNT(*) AS Quantidade
        FROM dbo.vw_dim_filiais
        GROUP BY [NomeFilial]
        HAVING COUNT(*) > 1;
    END
END
ELSE
BEGIN
    PRINT '   ⚠️ AVISO: View vw_dim_filiais não existe ainda';
END
GO

-- ============================================
-- Validação 3: vw_dim_veiculos (Chave: Placa)
-- ============================================
PRINT '';
PRINT '3. Validando vw_dim_veiculos (Chave: Placa)...';

IF EXISTS (SELECT 1 FROM sys.views WHERE name = 'vw_dim_veiculos' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DECLARE @VeiculosTotal INT;
    DECLARE @VeiculosDistinct INT;
    
    SELECT @VeiculosTotal = COUNT(*) FROM dbo.vw_dim_veiculos;
    SELECT @VeiculosDistinct = COUNT(DISTINCT Placa) FROM dbo.vw_dim_veiculos;
    
    IF @VeiculosTotal = @VeiculosDistinct
    BEGIN
        PRINT '   ✅ OK: vw_dim_veiculos - ' + CAST(@VeiculosTotal AS VARCHAR) + ' veículos únicos';
    END
    ELSE
    BEGIN
        PRINT '   ❌ ERRO: vw_dim_veiculos tem placas duplicadas!';
        PRINT '   Total de linhas: ' + CAST(@VeiculosTotal AS VARCHAR);
        PRINT '   Placas únicas: ' + CAST(@VeiculosDistinct AS VARCHAR);
        PRINT '   Placas duplicadas encontradas:';
        
        SELECT Placa, COUNT(*) AS Quantidade
        FROM dbo.vw_dim_veiculos
        GROUP BY Placa
        HAVING COUNT(*) > 1;
    END
END
ELSE
BEGIN
    PRINT '   ⚠️ AVISO: View vw_dim_veiculos não existe ainda';
END
GO

-- ============================================
-- Validação 4: vw_dim_motoristas (Chave: NomeMotorista)
-- ============================================
PRINT '';
PRINT '4. Validando vw_dim_motoristas (Chave: NomeMotorista)...';

IF EXISTS (SELECT 1 FROM sys.views WHERE name = 'vw_dim_motoristas' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DECLARE @MotoristasTotal INT;
    DECLARE @MotoristasDistinct INT;
    
    SELECT @MotoristasTotal = COUNT(*) FROM dbo.vw_dim_motoristas;
    SELECT @MotoristasDistinct = COUNT(DISTINCT NomeMotorista) FROM dbo.vw_dim_motoristas;
    
    IF @MotoristasTotal = @MotoristasDistinct
    BEGIN
        PRINT '   ✅ OK: vw_dim_motoristas - ' + CAST(@MotoristasTotal AS VARCHAR) + ' motoristas únicos';
    END
    ELSE
    BEGIN
        PRINT '   ❌ ERRO: vw_dim_motoristas tem nomes duplicados!';
        PRINT '   Total de linhas: ' + CAST(@MotoristasTotal AS VARCHAR);
        PRINT '   Nomes únicos: ' + CAST(@MotoristasDistinct AS VARCHAR);
        PRINT '   Nomes duplicados encontrados:';
        
        SELECT NomeMotorista, COUNT(*) AS Quantidade
        FROM dbo.vw_dim_motoristas
        GROUP BY NomeMotorista
        HAVING COUNT(*) > 1;
    END
END
ELSE
BEGIN
    PRINT '   ⚠️ AVISO: View vw_dim_motoristas não existe ainda';
END
GO

-- ============================================
-- Validação 5: vw_dim_planocontas (Chave: Descricao)
-- ============================================
PRINT '';
PRINT '5. Validando vw_dim_planocontas (Chave: Descricao)...';

IF EXISTS (SELECT 1 FROM sys.views WHERE name = 'vw_dim_planocontas' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DECLARE @PlanosTotal INT;
    DECLARE @PlanosDistinct INT;
    
    SELECT @PlanosTotal = COUNT(*) FROM dbo.vw_dim_planocontas;
    SELECT @PlanosDistinct = COUNT(DISTINCT Descricao) FROM dbo.vw_dim_planocontas;
    
    IF @PlanosTotal = @PlanosDistinct
    BEGIN
        PRINT '   ✅ OK: vw_dim_planocontas - ' + CAST(@PlanosTotal AS VARCHAR) + ' contas únicas';
    END
    ELSE
    BEGIN
        PRINT '   ❌ ERRO: vw_dim_planocontas tem descrições duplicadas!';
        PRINT '   Total de linhas: ' + CAST(@PlanosTotal AS VARCHAR);
        PRINT '   Descrições únicas: ' + CAST(@PlanosDistinct AS VARCHAR);
        PRINT '   Descrições duplicadas encontradas:';
        
        SELECT Descricao, COUNT(*) AS Quantidade
        FROM dbo.vw_dim_planocontas
        GROUP BY Descricao
        HAVING COUNT(*) > 1;
    END
END
ELSE
BEGIN
    PRINT '   ⚠️ AVISO: View vw_dim_planocontas não existe ainda';
END
GO

PRINT '';
PRINT '============================================';
PRINT 'VALIDAÇÃO CONCLUÍDA';
PRINT '============================================';
PRINT '';
PRINT 'IMPORTANTE: Se alguma view apresentou ERRO, corrija antes de usar no Power BI.';
PRINT 'Relacionamentos muitos-para-muitos não intencionais quebram modelos Star Schema.';
PRINT '';

-- ============================================
-- Script de configuração de permissões do usuário da aplicação
-- Execute este script APÓS criar as tabelas (scripts 001-010)
-- 
-- PRINCÍPIO DO MENOR PRIVILÉGIO:
-- O usuário da aplicação NÃO precisa de permissões DDL (CREATE, ALTER, DROP)
-- Apenas precisa de DML: SELECT, INSERT, UPDATE, DELETE (MERGE usa essas operações)
-- ============================================

-- NOTA: Este script configura permissões para o usuário 'sa' (padrão do config.bat)
-- Em produção, substitua 'sa' pelo nome do usuário da aplicação
-- O usuário 'sa' é sysadmin e não precisa de permissões adicionais

DECLARE @UsuarioAplicacao NVARCHAR(128) = 'sa';
DECLARE @Sql NVARCHAR(MAX);

PRINT '============================================';
PRINT 'Configurando permissões para usuário: ' + @UsuarioAplicacao;
PRINT '============================================';
PRINT '';

-- Verificar se o usuário conectado é sysadmin (não precisa de permissões)
IF IS_SRVROLEMEMBER('sysadmin') = 1
BEGIN
    PRINT 'O usuário conectado é sysadmin. Permissões adicionais não são necessárias.';
    PRINT 'Este script é útil apenas para usuários não-admin em produção.';
    PRINT '';
    PRINT '============================================';
    PRINT 'Script de configuração de permissões concluído.';
    PRINT '============================================';
END
ELSE
BEGIN
    -- Verificar se o usuário existe no banco
    IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = @UsuarioAplicacao)
    BEGIN
        PRINT 'AVISO: Usuário ' + @UsuarioAplicacao + ' não existe no banco de dados atual.';
        PRINT '       Este script configura permissões apenas para usuários existentes.';
        PRINT '       Se você está usando autenticação SQL Server, crie o usuário primeiro.';
        PRINT '';
    END
    ELSE
    BEGIN
        -- Remover permissões DDL se existirem (segurança) - idempotente
        BEGIN TRY
            SET @Sql = 'REVOKE CREATE TABLE FROM [' + @UsuarioAplicacao + '];';
            EXEC sp_executesql @Sql;
        END TRY
        BEGIN CATCH
            -- Ignora se a permissão não existe ou não pode ser revogada
        END CATCH
        
        BEGIN TRY
            SET @Sql = 'REVOKE ALTER ON SCHEMA::dbo FROM [' + @UsuarioAplicacao + '];';
            EXEC sp_executesql @Sql;
        END TRY
        BEGIN CATCH
            -- Ignora se a permissão não existe ou não pode ser revogada
        END CATCH

        -- Conceder apenas permissões DML necessárias (idempotente)
        IF NOT EXISTS (
            SELECT * FROM sys.database_role_members rm
            INNER JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
            INNER JOIN sys.database_principals m ON rm.member_principal_id = m.principal_id
            WHERE r.name = 'db_datareader' AND m.name = @UsuarioAplicacao
        )
        BEGIN
            SET @Sql = 'ALTER ROLE db_datareader ADD MEMBER [' + @UsuarioAplicacao + '];';
            EXEC sp_executesql @Sql;
            PRINT 'OK: Usuário ' + @UsuarioAplicacao + ' adicionado à role db_datareader.';
        END
        ELSE
        BEGIN
            PRINT 'OK: Usuário ' + @UsuarioAplicacao + ' já é membro da role db_datareader.';
        END

        IF NOT EXISTS (
            SELECT * FROM sys.database_role_members rm
            INNER JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
            INNER JOIN sys.database_principals m ON rm.member_principal_id = m.principal_id
            WHERE r.name = 'db_datawriter' AND m.name = @UsuarioAplicacao
        )
        BEGIN
            SET @Sql = 'ALTER ROLE db_datawriter ADD MEMBER [' + @UsuarioAplicacao + '];';
            EXEC sp_executesql @Sql;
            PRINT 'OK: Usuário ' + @UsuarioAplicacao + ' adicionado à role db_datawriter.';
        END
        ELSE
        BEGIN
            PRINT 'OK: Usuário ' + @UsuarioAplicacao + ' já é membro da role db_datawriter.';
        END
    END

    PRINT '';
    PRINT '============================================';
    PRINT 'Script de configuração de permissões concluído.';
    PRINT '============================================';
END
GO

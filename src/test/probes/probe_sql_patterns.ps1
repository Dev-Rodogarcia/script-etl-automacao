Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Load-EnvFile {
    param([string]$Path)

    $cfg = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $key, $value = $line -split "=", 2
        $cfg[$key.Trim()] = $value.Trim()
    }
    return $cfg
}

function Invoke-SqlText {
    param(
        [hashtable]$Config,
        [string]$Query
    )

    $database = ([regex]::Match($Config["DB_URL"], "databaseName=([^;]+)").Groups[1].Value)
    $server = "tcp:localhost,1433"
    $sqlcmd = "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\180\Tools\Binn\SQLCMD.EXE"

    & $sqlcmd `
        -S $server `
        -d $database `
        -U $Config["DB_USER"] `
        -P $Config["DB_PASSWORD"] `
        -C `
        -W `
        -s "|" `
        -Q $Query
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$cfg = Load-EnvFile -Path (Join-Path $root ".env")

$query = @"
SET NOCOUNT ON;

SELECT
    'role_probe' AS probe,
    ORIGINAL_LOGIN() AS login_name,
    SUSER_SNAME() AS suser_name,
    IS_SRVROLEMEMBER('sysadmin') AS is_sysadmin,
    HAS_PERMS_BY_NAME(DB_NAME(), 'DATABASE', 'ALTER') AS can_alter_db;

SELECT
    'schema_probe' AS probe,
    (SELECT COUNT(*) FROM sys.foreign_keys WHERE parent_object_id IN (OBJECT_ID('dbo.coletas'), OBJECT_ID('dbo.fretes'), OBJECT_ID('dbo.manifestos'), OBJECT_ID('dbo.contas_a_pagar'), OBJECT_ID('dbo.faturas_graphql'), OBJECT_ID('dbo.faturas_por_cliente'), OBJECT_ID('dbo.localizacao_cargas'), OBJECT_ID('dbo.cotacoes'), OBJECT_ID('dbo.inventario'), OBJECT_ID('dbo.sinistros'))) AS managed_foreign_keys,
    CASE WHEN OBJECT_ID('dbo.sys_execution_audit','U') IS NOT NULL THEN 1 ELSE 0 END AS sys_execution_audit_exists,
    CASE WHEN OBJECT_ID('dbo.sys_execution_watermark','U') IS NOT NULL THEN 1 ELSE 0 END AS sys_execution_watermark_exists;

IF OBJECT_ID('dbo.codex_fk_parent','U') IS NOT NULL DROP TABLE dbo.codex_fk_parent;
IF OBJECT_ID('dbo.codex_fk_child_no_fk','U') IS NOT NULL DROP TABLE dbo.codex_fk_child_no_fk;
IF OBJECT_ID('dbo.codex_fk_child_with_fk','U') IS NOT NULL DROP TABLE dbo.codex_fk_child_with_fk;
IF OBJECT_ID('dbo.codex_atomic_target','U') IS NOT NULL DROP TABLE dbo.codex_atomic_target;
IF OBJECT_ID('dbo.codex_atomic_stage','U') IS NOT NULL DROP TABLE dbo.codex_atomic_stage;
IF OBJECT_ID('dbo.codex_holdlock_target','U') IS NOT NULL DROP TABLE dbo.codex_holdlock_target;

CREATE TABLE dbo.codex_fk_parent (
    id INT NOT NULL PRIMARY KEY
);

CREATE TABLE dbo.codex_fk_child_no_fk (
    id INT NOT NULL PRIMARY KEY,
    parent_id INT NOT NULL
);

CREATE TABLE dbo.codex_fk_child_with_fk (
    id INT NOT NULL PRIMARY KEY,
    parent_id INT NOT NULL
);

ALTER TABLE dbo.codex_fk_child_with_fk
    ADD CONSTRAINT FK_codex_fk_child_with_fk_parent
    FOREIGN KEY (parent_id) REFERENCES dbo.codex_fk_parent(id);

DECLARE @orphanWithoutFkInserted BIT = 0;
DECLARE @orphanWithFkBlocked BIT = 0;

BEGIN TRY
    INSERT INTO dbo.codex_fk_child_no_fk (id, parent_id) VALUES (1, 999);
    SET @orphanWithoutFkInserted = 1;
END TRY
BEGIN CATCH
    SET @orphanWithoutFkInserted = 0;
END CATCH;

BEGIN TRY
    INSERT INTO dbo.codex_fk_child_with_fk (id, parent_id) VALUES (1, 999);
END TRY
BEGIN CATCH
    SET @orphanWithFkBlocked = 1;
END CATCH;

SELECT
    'fk_probe' AS probe,
    @orphanWithoutFkInserted AS orphan_without_fk_inserted,
    @orphanWithFkBlocked AS orphan_with_fk_blocked;

CREATE TABLE dbo.codex_atomic_target (
    id INT NOT NULL PRIMARY KEY,
    valor INT NOT NULL
);

CREATE TABLE dbo.codex_atomic_stage (
    id INT NOT NULL,
    valor INT NULL
);

DECLARE @directPartialRows INT = 0;
DECLARE @stagedRollbackRows INT = 0;

BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.codex_atomic_target (id, valor) VALUES (1, 10);
    COMMIT;

    BEGIN TRAN;
    INSERT INTO dbo.codex_atomic_target (id, valor) VALUES (2, 20);
    INSERT INTO dbo.codex_atomic_target (id, valor) VALUES (3, NULL);
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;

SELECT @directPartialRows = COUNT(*) FROM dbo.codex_atomic_target;

TRUNCATE TABLE dbo.codex_atomic_target;
INSERT INTO dbo.codex_atomic_stage (id, valor) VALUES (1, 10), (2, 20), (3, NULL);

BEGIN TRY
    BEGIN TRAN;
    IF EXISTS (SELECT 1 FROM dbo.codex_atomic_stage WHERE valor IS NULL)
        THROW 51000, 'Stage invalido detectado antes do merge final.', 1;

    INSERT INTO dbo.codex_atomic_target (id, valor)
    SELECT id, valor
    FROM dbo.codex_atomic_stage;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;

SELECT @stagedRollbackRows = COUNT(*) FROM dbo.codex_atomic_target;

SELECT
    'atomicity_probe' AS probe,
    @directPartialRows AS direct_partial_rows,
    @stagedRollbackRows AS staged_rollback_rows;

CREATE TABLE dbo.codex_holdlock_target (
    id INT NOT NULL PRIMARY KEY,
    valor INT NOT NULL
);

DECLARE @holdlockSyntaxOk BIT = 0;

BEGIN TRY
    MERGE dbo.codex_holdlock_target WITH (HOLDLOCK) AS target
    USING (SELECT CAST(1 AS INT) AS id, CAST(10 AS INT) AS valor) AS source
       ON target.id = source.id
    WHEN MATCHED THEN
        UPDATE SET valor = source.valor
    WHEN NOT MATCHED THEN
        INSERT (id, valor) VALUES (source.id, source.valor);

    SET @holdlockSyntaxOk = 1;
END TRY
BEGIN CATCH
    SET @holdlockSyntaxOk = 0;
END CATCH;

SELECT
    'holdlock_probe' AS probe,
    @holdlockSyntaxOk AS holdlock_syntax_ok;

DROP TABLE dbo.codex_fk_child_with_fk;
DROP TABLE dbo.codex_fk_child_no_fk;
DROP TABLE dbo.codex_fk_parent;
DROP TABLE dbo.codex_atomic_stage;
DROP TABLE dbo.codex_atomic_target;
DROP TABLE dbo.codex_holdlock_target;
"@

Invoke-SqlText -Config $cfg -Query $query

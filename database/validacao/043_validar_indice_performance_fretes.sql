-- Valida o indice de cobertura usado pelo Dashboard de Performance.
-- Script somente leitura: nao cria, altera ou remove objetos.

SET NOCOUNT ON;

DECLARE @Falhas TABLE (
    tipo NVARCHAR(50) NOT NULL,
    nome NVARCHAR(255) NOT NULL,
    detalhe NVARCHAR(500) NULL
);

IF OBJECT_ID(N'dbo.fretes', N'U') IS NULL
BEGIN
    INSERT INTO @Falhas VALUES (N'TABELA', N'dbo.fretes', N'Tabela base da vw_fretes_powerbi ausente');
END;
ELSE
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes i
        INNER JOIN sys.index_columns ic
            ON ic.object_id = i.object_id
           AND ic.index_id = i.index_id
           AND ic.key_ordinal = 1
        INNER JOIN sys.columns c
            ON c.object_id = ic.object_id
           AND c.column_id = ic.column_id
        WHERE i.object_id = OBJECT_ID(N'dbo.fretes')
          AND i.name = N'IX_fretes_performance_minuta_cobertura'
          AND i.type_desc = N'NONCLUSTERED'
          AND i.is_disabled = 0
          AND c.name = N'corporation_sequence_number'
    )
        INSERT INTO @Falhas VALUES (
            N'INDICE',
            N'IX_fretes_performance_minuta_cobertura',
            N'Indice ausente, desabilitado ou sem corporation_sequence_number como primeira chave'
        );

    DECLARE @IncludesObrigatorios TABLE (nome SYSNAME NOT NULL);
    INSERT INTO @IncludesObrigatorios (nome) VALUES
        (N'data_previsao_entrega'),
        (N'finished_at'),
        (N'fit_dpn_performance_finished_at'),
        (N'pagador_nome'),
        (N'filial_nome'),
        (N'filial_nome_key'),
        (N'destino_cidade'),
        (N'destino_uf'),
        (N'status'),
        (N'taxed_weight'),
        (N'valor_notas'),
        (N'data_extracao'),
        (N'excluido_na_origem');

    INSERT INTO @Falhas (tipo, nome, detalhe)
    SELECT
        N'INDICE_INCLUDE',
        N'IX_fretes_performance_minuta_cobertura.' + esperado.nome,
        N'Coluna de cobertura ausente'
    FROM @IncludesObrigatorios esperado
    WHERE NOT EXISTS (
        SELECT 1
        FROM sys.indexes i
        INNER JOIN sys.index_columns ic
            ON ic.object_id = i.object_id
           AND ic.index_id = i.index_id
           AND ic.is_included_column = 1
        INNER JOIN sys.columns c
            ON c.object_id = ic.object_id
           AND c.column_id = ic.column_id
        WHERE i.object_id = OBJECT_ID(N'dbo.fretes')
          AND i.name = N'IX_fretes_performance_minuta_cobertura'
          AND c.name = esperado.nome
    );
END;

SELECT tipo, nome, detalhe
FROM @Falhas
ORDER BY tipo, nome;

IF EXISTS (SELECT 1 FROM @Falhas)
    THROW 51072, 'Indice de Performance de fretes invalido. Verifique database/validacao/043_validar_indice_performance_fretes.sql.', 1;

PRINT 'Indice de Performance de fretes validado com sucesso.';
GO

-- ============================================================================
-- Tabela fato materializada para Gestao a Vista: Manifestos
-- ============================================================================

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_manifestos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.fato_gestao_vista_manifestos (
        sequence_code BIGINT NOT NULL,
        numero_manifesto BIGINT NOT NULL,
        identificador_unico NVARCHAR(100) NULL,

        data_criacao DATETIMEOFFSET NULL,
        data_criacao_date DATE NULL,
        data_criacao_yyyymm INT NULL,
        hora_solicitacao TIME(0) NULL,
        hora_criacao TIME(0) NULL,
        saida DATETIMEOFFSET NULL,
        fechamento DATETIMEOFFSET NULL,
        chegada DATETIMEOFFSET NULL,

        status NVARCHAR(100) NULL,
        status_raw NVARCHAR(50) NULL,
        status_key NVARCHAR(100) NULL,
        classificacao NVARCHAR(255) NULL,
        classificacao_bucket NVARCHAR(50) NULL,
        filial NVARCHAR(255) NULL,
        filial_emissora NVARCHAR(255) NULL,
        filial_key NVARCHAR(255) NULL,

        mdfe_number INT NULL,
        mdfe_key NVARCHAR(100) NULL,
        mdfe_status NVARCHAR(100) NULL,
        distribution_pole NVARCHAR(255) NULL,

        placa_veiculo NVARCHAR(20) NULL,
        placa_veiculo_key NVARCHAR(20) NULL,
        tipo_veiculo NVARCHAR(255) NULL,
        proprietario_nome NVARCHAR(255) NULL,
        proprietario_documento NVARCHAR(255) NULL,
        tipo_motorista NVARCHAR(100) NULL,
        tipo_motorista_key NVARCHAR(100) NULL,
        motorista NVARCHAR(255) NULL,
        motorista_key NVARCHAR(255) NULL,

        km_saida INT NULL,
        km_chegada INT NULL,
        km_viagem INT NULL,
        km_manual NVARCHAR(30) NULL,
        km_total DECIMAL(18, 2) NULL,

        qtd_nf INT NULL,
        volumes_nf INT NULL,
        peso_nf DECIMAL(18, 3) NULL,
        peso_taxado DECIMAL(18, 3) NULL,
        total_m3 DECIMAL(18, 6) NULL,
        valor_nf DECIMAL(18, 2) NULL,
        fretes_total DECIMAL(18, 2) NULL,
        coletas_total DECIMAL(18, 2) NULL,
        receita_total DECIMAL(18, 2) NULL,
        coleta_numeros NVARCHAR(MAX) NULL,

        contract_number NVARCHAR(50) NULL,
        tipo_contrato_veiculo NVARCHAR(100) NULL,
        tipo_contrato_veiculo_key NVARCHAR(100) NULL,
        tipo_contrato_motorista NVARCHAR(100) NULL,
        tipo_contrato_motorista_key NVARCHAR(100) NULL,
        tipo_contrato NVARCHAR(100) NULL,
        tipo_contrato_key NVARCHAR(100) NULL,
        tipo_calculo NVARCHAR(100) NULL,
        tipo_carga NVARCHAR(100) NULL,
        tipo_carga_key NVARCHAR(100) NULL,

        diaria DECIMAL(18, 2) NULL,
        custo_total DECIMAL(18, 2) NULL,
        valor_frete DECIMAL(18, 2) NULL,
        combustivel DECIMAL(18, 2) NULL,
        pedagio DECIMAL(18, 2) NULL,
        servicos_motorista_total DECIMAL(18, 2) NULL,
        despesa_operacional DECIMAL(18, 2) NULL,
        inss_value DECIMAL(18, 2) NULL,
        sest_senat_value DECIMAL(18, 2) NULL,
        ir_value DECIMAL(18, 2) NULL,
        saldo_pagar DECIMAL(18, 2) NULL,

        uniq_destinations_count INT NULL,
        gerar_mdfe BIT NULL,
        solicitou_monitoramento BIT NULL,
        solicitacao_monitoramento NVARCHAR(10) NULL,
        leitura_movel_em DATETIMEOFFSET NULL,

        itens_entrega INT NULL,
        itens_transferencia INT NULL,
        itens_coleta INT NULL,
        itens_despacho_rascunho INT NULL,
        itens_consolidacao INT NULL,
        itens_coleta_reversa INT NULL,
        itens_total INT NULL,
        itens_finalizados INT NULL,
        calculado_coleta INT NULL,
        calculado_entrega INT NULL,
        calculado_despacho INT NULL,
        calculado_consolidacao INT NULL,
        calculado_coleta_reversa INT NULL,

        valor_coletas DECIMAL(18, 2) NULL,
        valor_entregas DECIMAL(18, 2) NULL,
        despachos DECIMAL(18, 2) NULL,
        consolidacoes DECIMAL(18, 2) NULL,
        coleta_reversa DECIMAL(18, 2) NULL,
        adiantamento DECIMAL(18, 2) NULL,
        custos_frota DECIMAL(18, 2) NULL,
        adicionais DECIMAL(18, 2) NULL,
        descontos DECIMAL(18, 2) NULL,
        desconto_valor DECIMAL(18, 2) NULL,
        ajuste_comentarios NVARCHAR(MAX) NULL,

        iks_id NVARCHAR(100) NULL,
        programacao_sequence_code NVARCHAR(50) NULL,
        programacao_inicio DATETIMEOFFSET NULL,
        programacao_termino DATETIMEOFFSET NULL,
        carreta1_placa NVARCHAR(20) NULL,
        carreta1_capacidade_peso DECIMAL(18, 2) NULL,
        carreta2_placa NVARCHAR(20) NULL,
        carreta2_capacidade_peso DECIMAL(18, 2) NULL,
        veiculo_capacidade_peso DECIMAL(18, 2) NULL,
        veiculo_peso_cubado DECIMAL(18, 2) NULL,
        capacidade_lotacao_kg DECIMAL(18, 2) NULL,

        descarregamento_destinatarios NVARCHAR(MAX) NULL,
        local_descarregamento NVARCHAR(MAX) NULL,
        entrega_regioes NVARCHAR(MAX) NULL,
        programacao_cliente NVARCHAR(255) NULL,
        programacao_tipo_servico NVARCHAR(255) NULL,
        usuario_emissor NVARCHAR(255) NULL,
        usuario_ajuste NVARCHAR(255) NULL,
        obs_operacional NVARCHAR(MAX) NULL,
        obs_financeira NVARCHAR(MAX) NULL,
        metadata NVARCHAR(MAX) NULL,

        data_extracao DATETIME2(0) NULL,
        excluido_na_origem BIT NOT NULL CONSTRAINT DF_fato_gv_manifestos_excluido DEFAULT (0),
        data_exclusao_origem DATETIME2(0) NULL,
        ultima_reconciliacao_origem_em DATETIME2(0) NULL,
        snapshot_em DATETIME2(0) NOT NULL CONSTRAINT DF_fato_gv_manifestos_snapshot DEFAULT SYSUTCDATETIME(),
        hash_linha BINARY(32) NULL,

        CONSTRAINT PK_fato_gv_manifestos PRIMARY KEY CLUSTERED (sequence_code),
        CONSTRAINT CK_fato_gv_manifestos_sequence_match CHECK (numero_manifesto = sequence_code)
    );

    PRINT 'Tabela dbo.fato_gestao_vista_manifestos criada.';
END
ELSE
BEGIN
    PRINT 'Tabela dbo.fato_gestao_vista_manifestos ja existe.';
END;
GO

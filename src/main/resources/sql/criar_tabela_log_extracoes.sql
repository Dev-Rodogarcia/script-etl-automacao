-- Tabela de controle de extrações para detectar extrações incompletas
CREATE TABLE log_extracoes (
    id BIGINT IDENTITY PRIMARY KEY,
    entidade NVARCHAR(50) NOT NULL,
    timestamp_inicio DATETIME2 NOT NULL,
    timestamp_fim DATETIME2 NOT NULL,
    status_final NVARCHAR(20) NOT NULL, -- 'COMPLETO', 'INCOMPLETO_LIMITE', 'ERRO_API'
    registros_extraidos INT NOT NULL,
    paginas_processadas INT NOT NULL,
    mensagem NVARCHAR(MAX),
    INDEX idx_entidade_timestamp (entidade, timestamp_fim DESC)
);

-- Comentários para documentação
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Tabela de controle para rastrear o status de cada extração de dados das APIs',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'log_extracoes';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Status da extração: COMPLETO (sucesso total), INCOMPLETO_LIMITE (parou por limite de páginas), ERRO_API (erro na API)',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'log_extracoes',
    @level2type = N'COLUMN', @level2name = N'status_final';
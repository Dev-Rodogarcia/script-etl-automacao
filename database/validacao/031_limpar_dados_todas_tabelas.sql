-- Um único Run: apaga dados de todas as tabelas de uma vez. Cuidado: irreversível.
EXEC ('
DELETE FROM dbo.log_extracoes;
DELETE FROM dbo.page_audit;
DELETE FROM dbo.coletas;
DELETE FROM dbo.fretes;
DELETE FROM dbo.faturas_graphql;
DELETE FROM dbo.faturas_por_cliente;
DELETE FROM dbo.manifestos;
DELETE FROM dbo.cotacoes;
DELETE FROM dbo.localizacao_cargas;
DELETE FROM dbo.contas_a_pagar;
DELETE FROM dbo.dim_usuarios;
');

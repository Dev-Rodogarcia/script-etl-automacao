SELECT
    t.name AS Tabela,
    MAX(CASE WHEN i.type_desc = 'CLUSTERED COLUMNSTORE' THEN 'SIM' ELSE 'NAO' END) AS Tem_Columnstore,
    MAX(CASE WHEN c.name = 'hash_linha' THEN 'SIM' ELSE 'NAO' END) AS Tem_Hash_Linha,
    MAX(CASE WHEN p.partition_number > 1 THEN 'SIM' ELSE 'NAO' END) AS Tabela_Particionada,
    SUM(p.rows) AS Total_Linhas
FROM sys.tables t
INNER JOIN sys.partitions p ON t.object_id = p.object_id AND p.index_id IN (0,1,5)
LEFT JOIN sys.indexes i ON t.object_id = i.object_id
LEFT JOIN sys.columns c ON t.object_id = c.object_id AND c.name = 'hash_linha'
WHERE t.name LIKE 'fato_%' OR t.name LIKE 'dim_%'
GROUP BY t.name
ORDER BY t.name;

Integração no ClienteApiDataExport.java
Adicione estas constantes e métodos à classe ClienteApiDataExport:
java// Adicionar à lista de constantes:
private static final int TEMPLATE_ID_FATURAS_POR_CLIENTE = 4924;
private static final String CAMPO_FATURAS_POR_CLIENTE = "service_at";
private static final String TABELA_FATURAS_POR_CLIENTE = "freights";

// Adicionar método público:
/**
 * Busca dados de Faturas por Cliente da API Data Export (últimas 24h).
 */
public ResultadoExtracao<FaturaPorClienteDTO> buscarFaturasPorCliente() {
    logger.info("Buscando Faturas por Cliente da API DataExport (últimas 24h)");
    Instant agora = Instant.now();
    Instant ontem = agora.minusSeconds(24 * 60 * 60);
    return buscarDadosGenericos(
        TEMPLATE_ID_FATURAS_POR_CLIENTE,
        TABELA_FATURAS_POR_CLIENTE,
        CAMPO_FATURAS_POR_CLIENTE,
        new TypeReference<List<FaturaPorClienteDTO>>() {},
        ontem,
        agora
    );
}
Integração no DataExportRunner.java
Adicione este bloco ao método executar():
javaThread.sleep(2000);

// Faturas por Cliente
System.out.println("\n💰 Extraindo Faturas por Cliente (últimas 24h)...");
final LocalDateTime inicioFaturasPorCliente = LocalDateTime.now();
try {
    final FaturaPorClienteRepository faturasPorClienteRepository = new FaturaPorClienteRepository();
    final FaturaPorClienteMapper faturasPorClienteMapper = new FaturaPorClienteMapper();
    
    final ResultadoExtracao<FaturaPorClienteDTO> resultadoFaturasPorCliente = 
        clienteApiDataExport.buscarFaturasPorCliente();
    final List<FaturaPorClienteDTO> faturasPorClienteDTO = resultadoFaturasPorCliente.getDados();
    
    System.out.println("✓ Extraídas: " + faturasPorClienteDTO.size() + " faturas por cliente" +
                      (resultadoFaturasPorCliente.isCompleto() ? "" : 
                       " (INCOMPLETO: " + resultadoFaturasPorCliente.getMotivoInterrupcao() + ")"));

    int totalUnicos = 0;
    int registrosSalvos = 0;
    final int registrosExtraidos = resultadoFaturasPorCliente.getRegistrosExtraidos();

    if (!faturasPorClienteDTO.isEmpty()) {
        final List<FaturaPorClienteEntity> faturasPorClienteEntities = faturasPorClienteDTO.stream()
            .map(faturasPorClienteMapper::toEntity)
            .collect(Collectors.toList());

        // Deduplicação por unique_id
        final List<FaturaPorClienteEntity> faturasPorClienteUnicas = 
            deduplicarFaturasPorCliente(faturasPorClienteEntities);
        totalUnicos = faturasPorClienteUnicas.size();

        if (faturasPorClienteEntities.size() != faturasPorClienteUnicas.size()) {
            final int duplicadosRemovidos = faturasPorClienteEntities.size() - faturasPorClienteUnicas.size();
            System.out.println("⚠️ Removidos " + duplicadosRemovidos + 
                             " duplicados da resposta da API antes de salvar");
            logger.warn("🔄 API retornou {} duplicados para faturas por cliente. " +
                       "Removidos antes de salvar. Total único: {}", 
                       duplicadosRemovidos, faturasPorClienteUnicas.size());
        }

        registrosSalvos = faturasPorClienteRepository.salvar(faturasPorClienteUnicas);
        System.out.println("✓ Processadas: " + registrosSalvos + "/" + totalUnicos + 
                         " faturas por cliente (INSERTs + UPDATEs)");
    }

    final LogExtracaoEntity.StatusExtracao statusFinal =
        resultadoFaturasPorCliente.isCompleto() ? 
        LogExtracaoEntity.StatusExtracao.COMPLETO : 
        LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;

    final String mensagem = resultadoFaturasPorCliente.isCompleto() ?
        ("Extração completa – extraídos " + registrosExtraidos + 
         " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
        ("Extração incompleta (" + resultadoFaturasPorCliente.getMotivoInterrupcao() + 
         ") – extraídos " + registrosExtraidos + 
         " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");

    final LogExtracaoEntity logFaturasPorCliente = new LogExtracaoEntity(
        "faturas_por_cliente_data_export",
        inicioFaturasPorCliente,
        LocalDateTime.now(),
        statusFinal,
        totalUnicos,
        resultadoFaturasPorCliente.getPaginasProcessadas(),
        mensagem
    );
    logExtracaoRepository.gravarLogExtracao(logFaturasPorCliente);

} catch (RuntimeException | java.sql.SQLException e) {
    final LogExtracaoEntity logErro = new LogExtracaoEntity(
        "faturas_por_cliente_data_export",
        inicioFaturasPorCliente,
        LocalDateTime.now(),
        "ERRO_API",
        0,
        0,
        "Erro: " + e.getMessage()
    );
    logExtracaoRepository.gravarLogExtracao(logErro);
    throw new RuntimeException("Falha na extração de faturas por cliente", e);
}

// Adicionar método de deduplicação:
private static List<FaturaPorClienteEntity> deduplicarFaturasPorCliente(
        final List<FaturaPorClienteEntity> lista) {
    if (lista == null || lista.isEmpty()) {
        return lista;
    }

    return lista.stream()
        .collect(Collectors.toMap(
            e -> {
                if (e.getUniqueId() == null || e.getUniqueId().trim().isEmpty()) {
                    throw new IllegalStateException(
                        "Fatura por cliente com unique_id NULL não pode ser deduplicada");
                }
                return e.getUniqueId();
            },
            e -> e,
            (primeiro, segundo) -> {
                logger.warn("⚠️ Duplicado detectado: unique_id={}", segundo.getUniqueId());
                return primeiro;
            }
        ))
        .values()
        .stream()
        .collect(Collectors.toList());
}
Resumo das Classes Criadas
✅ FaturaPorClienteDTO.java - DTO com 22 campos do JSON
✅ FaturaPorClienteMapper.java - Conversão com lógica de negócio (Locale.US, arrays, identificador único)
✅ FaturaPorClienteEntity.java - Entity com campos tipados corretamente (BigDecimal, LocalDate, OffsetDateTime)
✅ FaturaPorClienteRepository.java - MERGE usando unique_id como chave, com índices de performance
As classes seguem exatamente o padrão do código existente e implementam todas as regras de negócio especificadas na documentação! 🎉Tentar novamente
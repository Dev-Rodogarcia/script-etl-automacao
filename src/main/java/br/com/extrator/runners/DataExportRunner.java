package br.com.extrator.runners;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.CotacaoEntity;
import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import br.com.extrator.db.entity.ManifestoEntity;
import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.repository.CotacaoRepository;
import br.com.extrator.db.repository.LocalizacaoCargaRepository;
import br.com.extrator.db.repository.ManifestoRepository;
import br.com.extrator.db.repository.LogExtracaoRepository;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoDTO;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoMapper;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaMapper;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoDTO;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoMapper;

/**
 * Runner independente para a API Data Export (Manifestos, Cotações e Localização de Carga).
 */
public final class DataExportRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataExportRunner.class);

    private DataExportRunner() {}

    public static void executar(final LocalDate dataInicio) throws Exception {
        System.out.println("🔄 Executando runner DataExport...");

        br.com.extrator.util.CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiDataExport clienteApiDataExport = new ClienteApiDataExport();
        final ManifestoRepository manifestoRepository = new ManifestoRepository();
        final CotacaoRepository cotacaoRepository = new CotacaoRepository();
        final LocalizacaoCargaRepository localizacaoRepository = new LocalizacaoCargaRepository();
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();

        final ManifestoMapper manifestoMapper = new ManifestoMapper();
        final CotacaoMapper cotacaoMapper = new CotacaoMapper();
        final LocalizacaoCargaMapper localizacaoMapper = new LocalizacaoCargaMapper();

        // Garante que a tabela log_extracoes existe
        logExtracaoRepository.criarTabelaSeNaoExistir();

        // Manifestos
        System.out.println("\n🧾 Extraindo Manifestos (últimas 24h)...");
        LocalDateTime inicioManifestos = LocalDateTime.now();
        try {
            final ResultadoExtracao<ManifestoDTO> resultadoManifestos = clienteApiDataExport.buscarManifestos();
            final List<ManifestoDTO> manifestosDTO = resultadoManifestos.getDados();
            System.out.println("✓ Extraídos: " + manifestosDTO.size() + " manifestos" + 
                              (resultadoManifestos.isCompleto() ? "" : " (INCOMPLETO: " + resultadoManifestos.getMotivoInterrupcao() + ")"));
            
            int registrosSalvos = 0;
            int totalUnicos = 0;
            final int registrosExtraidos = resultadoManifestos.getRegistrosExtraidos();
            if (!manifestosDTO.isEmpty()) {
                final List<ManifestoEntity> manifestosEntities = manifestosDTO.stream()
                    .map(manifestoMapper::toEntity)
                    .collect(Collectors.toList());
                
                // Deduplicar registros (proteção contra duplicados da API)
                final List<ManifestoEntity> manifestosUnicos = deduplicarManifestos(manifestosEntities);
                totalUnicos = manifestosUnicos.size();
                
                // Log se houver duplicados removidos
                if (manifestosEntities.size() != manifestosUnicos.size()) {
                    final int duplicadosRemovidos = manifestosEntities.size() - manifestosUnicos.size();
                    System.out.println("⚠️ Removidos " + duplicadosRemovidos + " duplicados da resposta da API antes de salvar");
                    logger.warn("🔄 API retornou {} duplicados para manifestos. Removidos antes de salvar. Total único: {}", 
                        duplicadosRemovidos, manifestosUnicos.size());
                }
                
                registrosSalvos = manifestoRepository.salvar(manifestosUnicos);
                System.out.println("✓ Processados: " + registrosSalvos + "/" + totalUnicos + " manifestos (INSERTs + UPDATEs)");
            }
            
            // Registrar no log (status enum + quantidade extraída)
            // NOTA: "salvos" = operações bem-sucedidas (INSERTs + UPDATEs)
            // UPDATEs não adicionam novas linhas, apenas atualizam existentes
            // registrosExtraidos = quantidade retornada pela API (pode incluir duplicados)
            // totalUnicos = quantidade após deduplicação (registros únicos)
            // registrosSalvos = quantidade processada com sucesso (INSERTs + UPDATEs)
            final LogExtracaoEntity.StatusExtracao statusFinal = 
                resultadoManifestos.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;

            final String mensagem = resultadoManifestos.isCompleto() ?
                ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
                ("Extração incompleta (" + resultadoManifestos.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");

            final LogExtracaoEntity logManifestos = new LogExtracaoEntity(
                "manifestos",
                inicioManifestos,
                LocalDateTime.now(),
                statusFinal,
                totalUnicos, // ← CORRIGIDO: usar totalUnicos (após deduplicação) em vez de registrosExtraidos (bruto da API)
                resultadoManifestos.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logManifestos);

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                "manifestos",
                inicioManifestos,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de manifestos", e);
        }

        Thread.sleep(2000);

        // Cotações
        System.out.println("\n💹 Extraindo Cotações (últimas 24h)...");
        LocalDateTime inicioCotacoes = LocalDateTime.now();
        try {
            final ResultadoExtracao<CotacaoDTO> resultadoCotacoes = clienteApiDataExport.buscarCotacoes();
            final List<CotacaoDTO> cotacoesDTO = resultadoCotacoes.getDados();
            System.out.println("✓ Extraídas: " + cotacoesDTO.size() + " cotações" + 
                              (resultadoCotacoes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoCotacoes.getMotivoInterrupcao() + ")"));
            
            int registrosSalvos = 0;
            int totalUnicos = 0;
            final int registrosExtraidos = resultadoCotacoes.getRegistrosExtraidos();
            if (!cotacoesDTO.isEmpty()) {
                final List<CotacaoEntity> cotacoesEntities = cotacoesDTO.stream()
                    .map(cotacaoMapper::toEntity)
                    .collect(Collectors.toList());
                
                // Deduplicar registros (proteção contra duplicados da API)
                final List<CotacaoEntity> cotacoesUnicas = deduplicarCotacoes(cotacoesEntities);
                totalUnicos = cotacoesUnicas.size();
                
                // Log se houver duplicados removidos
                if (cotacoesEntities.size() != cotacoesUnicas.size()) {
                    final int duplicadosRemovidos = cotacoesEntities.size() - cotacoesUnicas.size();
                    System.out.println("⚠️ Removidos " + duplicadosRemovidos + " duplicados da resposta da API antes de salvar");
                    logger.warn("🔄 API retornou {} duplicados para cotações. Removidos antes de salvar. Total único: {}", 
                        duplicadosRemovidos, cotacoesUnicas.size());
                }
                
                registrosSalvos = cotacaoRepository.salvar(cotacoesUnicas);
                System.out.println("✓ Processadas: " + registrosSalvos + "/" + totalUnicos + " cotações (INSERTs + UPDATEs)");
            }
            
            // Registrar no log (status enum + quantidade extraída)
            // NOTA: "salvos" = operações bem-sucedidas (INSERTs + UPDATEs)
            // UPDATEs não adicionam novas linhas, apenas atualizam existentes
            // registrosExtraidos = quantidade retornada pela API (pode incluir duplicados)
            // totalUnicos = quantidade após deduplicação (registros únicos)
            // registrosSalvos = quantidade processada com sucesso (INSERTs + UPDATEs)
            final LogExtracaoEntity.StatusExtracao statusFinal = 
                resultadoCotacoes.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;

            final String mensagem = resultadoCotacoes.isCompleto() ?
                ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
                ("Extração incompleta (" + resultadoCotacoes.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");

            final LogExtracaoEntity logCotacoes = new LogExtracaoEntity(
                "cotacoes",
                inicioCotacoes,
                LocalDateTime.now(),
                statusFinal,
                totalUnicos, // ← CORRIGIDO: usar totalUnicos (após deduplicação) em vez de registrosExtraidos (bruto da API)
                resultadoCotacoes.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logCotacoes);

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                "cotacoes",
                inicioCotacoes,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de cotações", e);
        }

        Thread.sleep(2000);

        // Localização de Carga
        System.out.println("\n📍 Extraindo Localização de Carga (últimas 24h)...");
        LocalDateTime inicioLocalizacoes = LocalDateTime.now();
        try {
            final ResultadoExtracao<LocalizacaoCargaDTO> resultadoLocalizacoes = clienteApiDataExport.buscarLocalizacaoCarga();
            final List<LocalizacaoCargaDTO> localizacoesDTO = resultadoLocalizacoes.getDados();
            System.out.println("✓ Extraídas: " + localizacoesDTO.size() + " localizações" + 
                              (resultadoLocalizacoes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoLocalizacoes.getMotivoInterrupcao() + ")"));
            
            int registrosSalvos = 0;
            final int registrosExtraidos = resultadoLocalizacoes.getRegistrosExtraidos();
            if (!localizacoesDTO.isEmpty()) {
                final List<LocalizacaoCargaEntity> localizacoesEntities = localizacoesDTO.stream()
                    .map(localizacaoMapper::toEntity)
                    .collect(Collectors.toList());
                
                // Deduplicar registros (proteção contra duplicados da API)
                final List<LocalizacaoCargaEntity> localizacoesUnicas = deduplicarLocalizacoes(localizacoesEntities);
                
                // Log se houver duplicados removidos
                if (localizacoesEntities.size() != localizacoesUnicas.size()) {
                    final int duplicadosRemovidos = localizacoesEntities.size() - localizacoesUnicas.size();
                    System.out.println("⚠️ Removidos " + duplicadosRemovidos + " duplicados da resposta da API antes de salvar");
                    logger.warn("🔄 API retornou {} duplicados para localizações. Removidos antes de salvar. Total único: {}", 
                        duplicadosRemovidos, localizacoesUnicas.size());
                }
                
                registrosSalvos = localizacaoRepository.salvar(localizacoesUnicas);
                final int totalUnicos = localizacoesUnicas.size();
                System.out.println("✓ Processadas: " + registrosSalvos + "/" + totalUnicos + " localizações (INSERTs + UPDATEs)");
                
                // Registrar no log (status enum + quantidade extraída)
                // NOTA: "salvos" = operações bem-sucedidas (INSERTs + UPDATEs)
                // UPDATEs não adicionam novas linhas, apenas atualizam existentes
                // registrosExtraidos = quantidade retornada pela API (pode incluir duplicados)
                // totalUnicos = quantidade após deduplicação (registros únicos)
                // registrosSalvos = quantidade processada com sucesso (INSERTs + UPDATEs)
                final LogExtracaoEntity.StatusExtracao statusFinal = 
                    resultadoLocalizacoes.isCompleto() ? LogExtracaoEntity.StatusExtracao.COMPLETO : LogExtracaoEntity.StatusExtracao.INCOMPLETO_LIMITE;

                final String mensagem = resultadoLocalizacoes.isCompleto() ?
                    ("Extração completa – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)") :
                    ("Extração incompleta (" + resultadoLocalizacoes.getMotivoInterrupcao() + ") – extraídos " + registrosExtraidos + " (únicos: " + totalUnicos + "), processados " + registrosSalvos + " (INSERTs + UPDATEs)");

                LogExtracaoEntity logLocalizacoes = new LogExtracaoEntity(
                    "localizacao_cargas",
                    inicioLocalizacoes,
                    LocalDateTime.now(),
                    statusFinal,
                    totalUnicos, // ← CORRIGIDO: usar totalUnicos (após deduplicação) em vez de registrosExtraidos (bruto da API)
                    resultadoLocalizacoes.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logLocalizacoes);
            }

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                "localizacao_cargas",
                inicioLocalizacoes,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de localização de cargas", e);
        }
    }
    
    /**
     * Deduplica lista de manifestos removendo registros duplicados da API.
     * Usa chave composta (sequence_code + identificador_unico) para identificar duplicados.
     * Mantém o primeiro registro encontrado e descarta duplicados subsequentes.
     * 
     * @param manifestos Lista de manifestos a deduplicar
     * @return Lista deduplicada de manifestos
     */
    private static List<ManifestoEntity> deduplicarManifestos(final List<ManifestoEntity> manifestos) {
        if (manifestos == null || manifestos.isEmpty()) {
            return manifestos;
        }
        
        return manifestos.stream()
            .collect(Collectors.toMap(
                m -> {
                    // Chave única: sequence_code + "_" + identificador_unico
                    if (m.getSequenceCode() == null) {
                        throw new IllegalStateException("Manifesto com sequence_code NULL não pode ser deduplicado");
                    }
                    if (m.getIdentificadorUnico() == null || m.getIdentificadorUnico().trim().isEmpty()) {
                        throw new IllegalStateException("Manifesto com identificador_unico NULL/vazio não pode ser deduplicado");
                    }
                    return m.getSequenceCode() + "_" + m.getIdentificadorUnico();
                },
                m -> m,
                (primeiro, segundo) -> {
                    // Se houver duplicado, manter o primeiro e logar o segundo
                    logger.warn("⚠️ Duplicado detectado na resposta da API: sequence_code={}, identificador_unico={}", 
                        segundo.getSequenceCode(), segundo.getIdentificadorUnico());
                    return primeiro; // Mantém o primeiro
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Deduplica lista de cotações removendo registros duplicados da API.
     * Usa sequence_code como chave única (PRIMARY KEY da tabela).
     * Mantém o primeiro registro encontrado e descarta duplicados subsequentes.
     * 
     * @param cotacoes Lista de cotações a deduplicar
     * @return Lista deduplicada de cotações
     */
    private static List<CotacaoEntity> deduplicarCotacoes(final List<CotacaoEntity> cotacoes) {
        if (cotacoes == null || cotacoes.isEmpty()) {
            return cotacoes;
        }
        
        return cotacoes.stream()
            .collect(Collectors.toMap(
                c -> {
                    // Chave única: sequence_code
                    if (c.getSequenceCode() == null) {
                        throw new IllegalStateException("Cotação com sequence_code NULL não pode ser deduplicada");
                    }
                    return c.getSequenceCode();
                },
                c -> c,
                (primeiro, segundo) -> {
                    // Se houver duplicado, manter o primeiro e logar o segundo
                    logger.warn("⚠️ Duplicado detectado na resposta da API: sequence_code={}", 
                        segundo.getSequenceCode());
                    return primeiro; // Mantém o primeiro
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Deduplica lista de localizações removendo registros duplicados da API.
     * Usa sequence_number como chave única (PRIMARY KEY da tabela).
     * Mantém o primeiro registro encontrado e descarta duplicados subsequentes.
     * 
     * @param localizacoes Lista de localizações a deduplicar
     * @return Lista deduplicada de localizações
     */
    private static List<LocalizacaoCargaEntity> deduplicarLocalizacoes(final List<LocalizacaoCargaEntity> localizacoes) {
        if (localizacoes == null || localizacoes.isEmpty()) {
            return localizacoes;
        }
        
        return localizacoes.stream()
            .collect(Collectors.toMap(
                l -> {
                    // Chave única: sequence_number
                    if (l.getSequenceNumber() == null) {
                        throw new IllegalStateException("Localização com sequence_number NULL não pode ser deduplicada");
                    }
                    return l.getSequenceNumber();
                },
                l -> l,
                (primeiro, segundo) -> {
                    // Se houver duplicado, manter o primeiro e logar o segundo
                    logger.warn("⚠️ Duplicado detectado na resposta da API: sequence_number={}", 
                        segundo.getSequenceNumber());
                    return primeiro; // Mantém o primeiro
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
}
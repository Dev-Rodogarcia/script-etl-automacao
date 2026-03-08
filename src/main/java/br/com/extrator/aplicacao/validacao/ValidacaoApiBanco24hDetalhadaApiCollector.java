/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/validacao/ValidacaoApiBanco24hDetalhadaApiCollector.java
Classe  : ValidacaoApiBanco24hDetalhadaApiCollector (final class)
Pacote  : br.com.extrator.aplicacao.validacao
Modulo  : Use Case - Validacao

Papel   : Coleta dados de APIs (GraphQL, DataExport) para 14 entidades, mapeando DTOs a entities, deduplicando e gerando metadata hashes.

Conecta com:
- ClienteApiDataExport, ClienteApiGraphQL (integracao)
- Mappers (mapeamento.*): ManifestoMapper, CotacaoMapper, LocalizacaoCargaMapper, ContasAPagarMapper, FaturaPorClienteMapper, FreteMapper, ColetaMapper
- Deduplicator (integracao.dataexport.support)
- ValidacaoApiBanco24hDetalhadaMetadataHasher (para hashing de metadados)
- ValidacaoApiBanco24hDetalhadaRepository (para queries em banco)

Fluxo geral:
1) criarEntidades() retorna lista de 8 entidades (ou 9 com FATURAS_GRAPHQL).
2) Para cada entidade: carregaDados(), mapeia DTOs, deuplica, gera hashes por chave.
3) Retorna ResultadoApiChaves com contas (bruto, unico, invalidos), chaves, hashes, detalhe.

Estrutura interna:
Atributos-chave:
- clienteDataExport, clienteGraphQL: clientes API.
- mappers: diversos mappers de DTO -> Entity.
- metadataHasher: para SHA256 de metadados.
- repository: para consultas auxiliares (faturas orfaas).
Metodos principais:
- criarEntidades(Connection, LocalDate, LocalDate, boolean): cria lista com 8-9 entidades.
- carregarManifestos/Cotacoes/Localizacao/...(): metodos para cada entidade (DataExport).
- carregarFretes/Coletas(): metodos para GraphQL.
- carregarFaturasGraphQL(Connection, LocalDate, LocalDate): faturas com fallback para orfaas.
- carregarDataExport/carregarGraphQL(): templates genericos para coleta, mapeamento, deduplicacao, hashing.
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.validacao;

import static br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaTypes.EntidadeValidacao;
import static br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaTypes.ResultadoApiChaves;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import br.com.extrator.integracao.ClienteApiDataExport;
import br.com.extrator.integracao.ClienteApiGraphQL;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.persistencia.entidade.ColetaEntity;
import br.com.extrator.persistencia.entidade.ContasAPagarDataExportEntity;
import br.com.extrator.persistencia.entidade.CotacaoEntity;
import br.com.extrator.persistencia.entidade.FaturaPorClienteEntity;
import br.com.extrator.persistencia.entidade.FreteEntity;
import br.com.extrator.persistencia.entidade.LocalizacaoCargaEntity;
import br.com.extrator.persistencia.entidade.ManifestoEntity;
import br.com.extrator.integracao.mapeamento.dataexport.contasapagar.ContasAPagarMapper;
import br.com.extrator.integracao.mapeamento.dataexport.cotacao.CotacaoMapper;
import br.com.extrator.dominio.dataexport.faturaporcliente.FaturaPorClienteDTO;
import br.com.extrator.integracao.mapeamento.dataexport.faturaporcliente.FaturaPorClienteMapper;
import br.com.extrator.integracao.mapeamento.dataexport.localizacaocarga.LocalizacaoCargaMapper;
import br.com.extrator.integracao.mapeamento.dataexport.manifestos.ManifestoMapper;
import br.com.extrator.integracao.mapeamento.graphql.coletas.ColetaMapper;
import br.com.extrator.dominio.graphql.faturas.CreditCustomerBillingNodeDTO;
import br.com.extrator.integracao.mapeamento.graphql.fretes.FreteMapper;
import br.com.extrator.integracao.dataexport.support.Deduplicator;
import br.com.extrator.suporte.mapeamento.MapperUtil;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

final class ValidacaoApiBanco24hDetalhadaApiCollector {
    private static final int LIMITE_BACKFILL_FATURAS_ORFAAS = 2000;

    private final ClienteApiDataExport clienteDataExport;
    private final ClienteApiGraphQL clienteGraphQL;
    private final ManifestoMapper manifestoMapper;
    private final CotacaoMapper cotacaoMapper;
    private final LocalizacaoCargaMapper localizacaoMapper;
    private final ContasAPagarMapper contasMapper;
    private final FaturaPorClienteMapper faturaPorClienteMapper;
    private final FreteMapper freteMapper;
    private final ColetaMapper coletaMapper;
    private final ValidacaoApiBanco24hDetalhadaMetadataHasher metadataHasher;
    private final ValidacaoApiBanco24hDetalhadaRepository repository;

    ValidacaoApiBanco24hDetalhadaApiCollector(
        final ValidacaoApiBanco24hDetalhadaMetadataHasher metadataHasher,
        final ValidacaoApiBanco24hDetalhadaRepository repository
    ) {
        this(
            new ClienteApiDataExport(),
            new ClienteApiGraphQL(),
            new ManifestoMapper(),
            new CotacaoMapper(),
            new LocalizacaoCargaMapper(),
            new ContasAPagarMapper(),
            new FaturaPorClienteMapper(),
            new FreteMapper(),
            new ColetaMapper(),
            metadataHasher,
            repository
        );
    }

    ValidacaoApiBanco24hDetalhadaApiCollector(
        final ClienteApiDataExport clienteDataExport,
        final ClienteApiGraphQL clienteGraphQL,
        final ManifestoMapper manifestoMapper,
        final CotacaoMapper cotacaoMapper,
        final LocalizacaoCargaMapper localizacaoMapper,
        final ContasAPagarMapper contasMapper,
        final FaturaPorClienteMapper faturaPorClienteMapper,
        final FreteMapper freteMapper,
        final ColetaMapper coletaMapper,
        final ValidacaoApiBanco24hDetalhadaMetadataHasher metadataHasher,
        final ValidacaoApiBanco24hDetalhadaRepository repository
    ) {
        this.clienteDataExport = clienteDataExport;
        this.clienteGraphQL = clienteGraphQL;
        this.manifestoMapper = manifestoMapper;
        this.cotacaoMapper = cotacaoMapper;
        this.localizacaoMapper = localizacaoMapper;
        this.contasMapper = contasMapper;
        this.faturaPorClienteMapper = faturaPorClienteMapper;
        this.freteMapper = freteMapper;
        this.coletaMapper = coletaMapper;
        this.metadataHasher = metadataHasher;
        this.repository = repository;
    }

    List<EntidadeValidacao> criarEntidades(
        final Connection conexao,
        final LocalDate dataInicio,
        final LocalDate dataFim,
        final boolean incluirFaturasGraphQL
    ) {
        final List<EntidadeValidacao> entidades = new ArrayList<>();
        entidades.add(new EntidadeValidacao(ConstantesEntidades.MANIFESTOS, () -> carregarManifestos(dataInicio, dataFim)));
        entidades.add(new EntidadeValidacao(ConstantesEntidades.COTACOES, () -> carregarCotacoes(dataInicio, dataFim)));
        entidades.add(
            new EntidadeValidacao(ConstantesEntidades.LOCALIZACAO_CARGAS, () -> carregarLocalizacao(dataInicio, dataFim))
        );
        entidades.add(
            new EntidadeValidacao(ConstantesEntidades.CONTAS_A_PAGAR, () -> carregarContasAPagar(dataInicio, dataFim))
        );
        entidades.add(
            new EntidadeValidacao(
                ConstantesEntidades.FATURAS_POR_CLIENTE,
                () -> carregarFaturasPorCliente(dataInicio, dataFim)
            )
        );
        entidades.add(new EntidadeValidacao(ConstantesEntidades.FRETES, () -> carregarFretes(dataInicio, dataFim)));
        entidades.add(new EntidadeValidacao(ConstantesEntidades.COLETAS, () -> carregarColetas(dataInicio, dataFim)));
        if (incluirFaturasGraphQL) {
            entidades.add(
                new EntidadeValidacao(
                    ConstantesEntidades.FATURAS_GRAPHQL,
                    () -> carregarFaturasGraphQL(conexao, dataInicio, dataFim)
                )
            );
        }
        return entidades;
    }

    private ResultadoApiChaves carregarManifestos(final LocalDate dataInicio, final LocalDate dataFim) {
        return carregarDataExport(
            () -> clienteDataExport.buscarManifestos(dataInicio, dataFim),
            manifestoMapper::toEntity,
            entity -> entity != null && entity.getSequenceCode() != null,
            lista -> Deduplicator.deduplicarManifestos(lista),
            this::chaveManifesto,
            ManifestoEntity::getMetadata,
            ConstantesEntidades.MANIFESTOS
        );
    }

    private ResultadoApiChaves carregarCotacoes(final LocalDate dataInicio, final LocalDate dataFim) {
        return carregarDataExport(
            () -> clienteDataExport.buscarCotacoes(dataInicio, dataFim),
            cotacaoMapper::toEntity,
            entity -> entity != null && entity.getSequenceCode() != null,
            lista -> Deduplicator.deduplicarCotacoes(lista),
            entity -> String.valueOf(entity.getSequenceCode()),
            CotacaoEntity::getMetadata,
            ConstantesEntidades.COTACOES
        );
    }

    private ResultadoApiChaves carregarLocalizacao(final LocalDate dataInicio, final LocalDate dataFim) {
        return carregarDataExport(
            () -> clienteDataExport.buscarLocalizacaoCarga(dataInicio, dataFim),
            localizacaoMapper::toEntity,
            entity -> entity != null && entity.getSequenceNumber() != null,
            lista -> Deduplicator.deduplicarLocalizacoes(lista),
            entity -> String.valueOf(entity.getSequenceNumber()),
            LocalizacaoCargaEntity::getMetadata,
            ConstantesEntidades.LOCALIZACAO_CARGAS
        );
    }

    private ResultadoApiChaves carregarContasAPagar(final LocalDate dataInicio, final LocalDate dataFim) {
        return carregarDataExport(
            () -> clienteDataExport.buscarContasAPagar(dataInicio, dataFim),
            contasMapper::toEntity,
            entity -> entity != null && entity.getSequenceCode() != null,
            lista -> Deduplicator.deduplicarFaturasAPagar(lista),
            entity -> String.valueOf(entity.getSequenceCode()),
            ContasAPagarDataExportEntity::getMetadata,
            ConstantesEntidades.CONTAS_A_PAGAR
        );
    }

    private ResultadoApiChaves carregarFaturasPorCliente(final LocalDate dataInicio, final LocalDate dataFim) {
        final ResultadoExtracao<FaturaPorClienteDTO> resultado = clienteDataExport.buscarFaturasPorCliente(dataInicio, dataFim);
        final List<FaturaPorClienteDTO> dtos = resultado.getDados() != null ? resultado.getDados() : List.of();
        final int bruto = dtos.size();
        int invalidos = 0;
        final List<FaturaPorClienteEntity> mapeadas = new ArrayList<>();
        final Map<String, Set<String>> hashesAceitosPorChave = new LinkedHashMap<>();

        for (final FaturaPorClienteDTO dto : dtos) {
            try {
                final FaturaPorClienteEntity entity = faturaPorClienteMapper.toEntity(dto);
                if (entity == null || entity.getUniqueId() == null || entity.getUniqueId().isBlank()) {
                    invalidos++;
                    continue;
                }
                mapeadas.add(entity);
                hashesAceitosPorChave
                    .computeIfAbsent(entity.getUniqueId(), ignored -> new HashSet<>())
                    .add(metadataHasher.hashMetadata(ConstantesEntidades.FATURAS_POR_CLIENTE, entity.getMetadata()));
            } catch (RuntimeException e) {
                invalidos++;
            }
        }

        final List<FaturaPorClienteEntity> deduplicadas = Deduplicator.deduplicarFaturasPorCliente(mapeadas);
        final Map<String, String> hashesPorChave = new LinkedHashMap<>();
        for (final FaturaPorClienteEntity entity : deduplicadas) {
            hashesPorChave.put(
                entity.getUniqueId(),
                metadataHasher.hashMetadata(ConstantesEntidades.FATURAS_POR_CLIENTE, entity.getMetadata())
            );
        }

        int chavesComHashesConflitantes = 0;
        for (final Set<String> hashes : hashesAceitosPorChave.values()) {
            if (hashes != null && hashes.size() > 1) {
                chavesComHashesConflitantes++;
            }
        }

        return new ResultadoApiChaves(
            bruto,
            hashesPorChave.size(),
            invalidos,
            new HashSet<>(hashesPorChave.keySet()),
            hashesPorChave,
            hashesAceitosPorChave,
            chavesComHashesConflitantes > 0
                ? "chaves_com_hashes_conflitantes=" + chavesComHashesConflitantes
                : null,
            Set.of()
        );
    }

    private ResultadoApiChaves carregarFretes(final LocalDate dataInicio, final LocalDate dataFim) {
        return carregarGraphQL(
            () -> clienteGraphQL.buscarFretes(dataInicio, dataFim),
            freteMapper::toEntity,
            entity -> entity != null && entity.getId() != null,
            entity -> String.valueOf(entity.getId()),
            FreteEntity::getMetadata,
            ConstantesEntidades.FRETES
        );
    }

    private ResultadoApiChaves carregarColetas(final LocalDate dataInicio, final LocalDate dataFim) {
        return carregarGraphQL(
            () -> clienteGraphQL.buscarColetas(dataInicio, dataFim),
            coletaMapper::toEntity,
            entity -> entity != null && entity.getId() != null && !entity.getId().isBlank(),
            ColetaEntity::getId,
            ColetaEntity::getMetadata,
            ConstantesEntidades.COLETAS
        );
    }

    private ResultadoApiChaves carregarFaturasGraphQL(
        final Connection conexao,
        final LocalDate dataInicio,
        final LocalDate dataFim
    ) throws SQLException {
        final ResultadoExtracao<CreditCustomerBillingNodeDTO> resultado =
            clienteGraphQL.buscarCapaFaturas(dataInicio, dataFim);
        final List<CreditCustomerBillingNodeDTO> dtos = resultado.getDados() != null ? resultado.getDados() : List.of();
        final int bruto = dtos.size();
        int invalidos = 0;

        final Map<Long, CreditCustomerBillingNodeDTO> porId = new LinkedHashMap<>();
        for (final CreditCustomerBillingNodeDTO dto : dtos) {
            if (dto == null || dto.getId() == null) {
                invalidos++;
                continue;
            }
            porId.put(dto.getId(), dto);
        }

        final List<Long> idsFretesJanela = repository.listarAccountingCreditIdsFretes(
            conexao,
            dataInicio,
            dataFim,
            LIMITE_BACKFILL_FATURAS_ORFAAS
        );
        final Map<String, String> hashesPorChave = new LinkedHashMap<>();
        for (final Map.Entry<Long, CreditCustomerBillingNodeDTO> entry : porId.entrySet()) {
            hashesPorChave.put(
                String.valueOf(entry.getKey()),
                metadataHasher.hashMetadata(
                    ConstantesEntidades.FATURAS_GRAPHQL,
                    MapperUtil.toJson(entry.getValue())
                )
            );
        }

        return new ResultadoApiChaves(
            bruto,
            hashesPorChave.size(),
            invalidos,
            new HashSet<>(hashesPorChave.keySet()),
            hashesPorChave,
            Map.of(),
            "ids_fretes_janela=" + idsFretesJanela.size() + " | tolerancia_excedentes_referenciais_ativa=true",
            idsFretesJanela.stream().map(String::valueOf).collect(Collectors.toSet())
        );
    }

    private <DTO, ENTITY> ResultadoApiChaves carregarDataExport(
        final Supplier<ResultadoExtracao<DTO>> extracaoSupplier,
        final Function<DTO, ENTITY> mapper,
        final Predicate<ENTITY> entidadeValida,
        final UnaryOperator<List<ENTITY>> deduplicador,
        final Function<ENTITY, String> chaveResolver,
        final Function<ENTITY, String> metadataResolver,
        final String entidade
    ) {
        final ResultadoExtracao<DTO> resultado = extracaoSupplier.get();
        final List<DTO> dtos = resultado.getDados() != null ? resultado.getDados() : List.of();
        final int bruto = dtos.size();
        int invalidos = 0;
        final List<ENTITY> mapeadas = new ArrayList<>();

        for (final DTO dto : dtos) {
            try {
                final ENTITY entity = mapper.apply(dto);
                if (!entidadeValida.test(entity)) {
                    invalidos++;
                    continue;
                }
                mapeadas.add(entity);
            } catch (RuntimeException e) {
                invalidos++;
            }
        }

        final List<ENTITY> deduplicadas = deduplicador.apply(mapeadas);
        final Map<String, String> hashesPorChave = new LinkedHashMap<>();
        for (final ENTITY entity : deduplicadas) {
            hashesPorChave.put(
                chaveResolver.apply(entity),
                metadataHasher.hashMetadata(entidade, metadataResolver.apply(entity))
            );
        }

        return new ResultadoApiChaves(
            bruto,
            hashesPorChave.size(),
            invalidos,
            new HashSet<>(hashesPorChave.keySet()),
            hashesPorChave,
            Map.of(),
            null,
            Set.of()
        );
    }

    private <DTO, ENTITY> ResultadoApiChaves carregarGraphQL(
        final Supplier<ResultadoExtracao<DTO>> extracaoSupplier,
        final Function<DTO, ENTITY> mapper,
        final Predicate<ENTITY> entidadeValida,
        final Function<ENTITY, String> chaveResolver,
        final Function<ENTITY, String> metadataResolver,
        final String entidade
    ) {
        final ResultadoExtracao<DTO> resultado = extracaoSupplier.get();
        final List<DTO> dtos = resultado.getDados() != null ? resultado.getDados() : List.of();
        final int bruto = dtos.size();
        int invalidos = 0;
        final Map<String, String> hashesPorChave = new LinkedHashMap<>();

        for (final DTO dto : dtos) {
            try {
                final ENTITY entity = mapper.apply(dto);
                if (!entidadeValida.test(entity)) {
                    invalidos++;
                    continue;
                }
                hashesPorChave.put(
                    chaveResolver.apply(entity),
                    metadataHasher.hashMetadata(entidade, metadataResolver.apply(entity))
                );
            } catch (RuntimeException e) {
                invalidos++;
            }
        }

        return new ResultadoApiChaves(
            bruto,
            hashesPorChave.size(),
            invalidos,
            new HashSet<>(hashesPorChave.keySet()),
            hashesPorChave,
            Map.of(),
            null,
            Set.of()
        );
    }

    private String chaveManifesto(final ManifestoEntity entity) {
        final long pick = entity.getPickSequenceCode() != null ? entity.getPickSequenceCode() : -1L;
        final int mdfe = entity.getMdfeNumber() != null ? entity.getMdfeNumber() : -1;
        return entity.getSequenceCode() + "|" + pick + "|" + mdfe;
    }
}

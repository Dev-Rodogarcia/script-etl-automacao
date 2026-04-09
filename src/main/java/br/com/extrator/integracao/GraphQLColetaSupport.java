package br.com.extrator.integracao;

/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/integracao/GraphQLColetaSupport.java
Classe  :  (class)
Pacote  : br.com.extrator.integracao
Modulo  : Integracao HTTP
Papel   : [DESC PENDENTE]
Conecta com: Sem dependencia interna
Fluxo geral:
1) [PENDENTE]
Estrutura interna:
Metodos: [PENDENTE]
Atributos: [PENDENTE]
[DOC-FILE-END]============================================================== */


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;

import br.com.extrator.integracao.constantes.ConstantesApiGraphQL;
import br.com.extrator.dominio.graphql.coletas.ColetaNodeDTO;
import br.com.extrator.integracao.graphql.GraphQLQueries;
import br.com.extrator.suporte.formatacao.FormatadorData;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

final class GraphQLColetaSupport {
    private final Logger logger;
    private final GraphQLPaginator paginator;

    GraphQLColetaSupport(final Logger logger,
                        final GraphQLSchemaInspector schemaInspector,
                        final GraphQLPaginator paginator) {
        this.logger = logger;
        this.paginator = paginator;
    }

    ResultadoExtracao<ColetaNodeDTO> buscarColetas(final String executionUuid,
                                                   final LocalDate dataInicio,
                                                   final LocalDate dataFim) {
        logger.info("ℹ️ Coletas: consultando exclusivamente por requestDate.");
        return GraphQLIntervaloHelper.executarPorDia(dataInicio, dataFim, data -> buscarColetasDia(executionUuid, data), "Coletas");
    }

    private ResultadoExtracao<ColetaNodeDTO> buscarColetasDia(final String executionUuid, final LocalDate data) {
        return buscarColetasDiaComCampo(executionUuid, data, "requestDate");
    }

    private ResultadoExtracao<ColetaNodeDTO> buscarColetasDiaComCampo(final String executionUuid,
                                                                      final LocalDate data,
                                                                      final String campoData) {
        final Map<String, Object> variaveis = Map.of("params", Map.of(campoData, data.format(FormatadorData.ISO_DATE)));
        return paginator.executarQueryPaginada(
            executionUuid,
            GraphQLQueries.QUERY_COLETAS,
            ConstantesApiGraphQL.obterNomeEntidadeApi(ConstantesEntidades.COLETAS),
            variaveis,
            ColetaNodeDTO.class
        );
    }

    static List<ColetaNodeDTO> deduplicarColetasPorId(final List<ColetaNodeDTO> coletas) {
        final Map<String, ColetaNodeDTO> unicos = new LinkedHashMap<>();
        for (final ColetaNodeDTO coleta : coletas) {
            if (coleta == null) {
                continue;
            }
            final String chave = resolverChaveDeduplicacao(coleta);
            unicos.merge(chave, coleta, GraphQLColetaSupport::preferirRegistroMaisFresco);
        }
        return new ArrayList<>(unicos.values());
    }

    static String resolverChaveDeduplicacao(final ColetaNodeDTO coleta) {
        if (coleta == null) {
            throw new IllegalArgumentException("Coleta nula nao pode ser deduplicada.");
        }
        if (coleta.getId() != null && !coleta.getId().isBlank()) {
            return coleta.getId();
        }
        if (coleta.getSequenceCode() != null) {
            return "SEQ:" + coleta.getSequenceCode();
        }
        throw new IllegalStateException("Coleta sem id e sem sequenceCode nao pode ser deduplicada.");
    }

    static ColetaNodeDTO preferirRegistroMaisFresco(final ColetaNodeDTO atual, final ColetaNodeDTO candidata) {
        final int comparacao = compararFrescor(candidata, atual);
        if (comparacao > 0) {
            return candidata;
        }
        if (comparacao < 0) {
            return atual;
        }
        return desempateDeterministico(atual, candidata) >= 0 ? atual : candidata;
    }

    static int compararFrescor(final ColetaNodeDTO esquerda, final ColetaNodeDTO direita) {
        final int statusUpdatedAt = compararData(parseData(esquerda == null ? null : esquerda.getStatusUpdatedAt()),
            parseData(direita == null ? null : direita.getStatusUpdatedAt()));
        if (statusUpdatedAt != 0) {
            return statusUpdatedAt;
        }
        final int finishDate = compararData(parseData(esquerda == null ? null : esquerda.getFinishDate()),
            parseData(direita == null ? null : direita.getFinishDate()));
        if (finishDate != 0) {
            return finishDate;
        }
        final int serviceDate = compararData(parseData(esquerda == null ? null : esquerda.getServiceDate()),
            parseData(direita == null ? null : direita.getServiceDate()));
        if (serviceDate != 0) {
            return serviceDate;
        }
        return compararData(parseData(esquerda == null ? null : esquerda.getRequestDate()),
            parseData(direita == null ? null : direita.getRequestDate()));
    }

    private static OffsetDateTime parseData(final String valor) {
        return FormatadorData.parseOffsetDateTime(valor);
    }

    private static int compararData(final OffsetDateTime esquerda, final OffsetDateTime direita) {
        if (esquerda == null && direita == null) {
            return 0;
        }
        if (esquerda == null) {
            return -1;
        }
        if (direita == null) {
            return 1;
        }
        return esquerda.compareTo(direita);
    }

    private static int desempateDeterministico(final ColetaNodeDTO atual, final ColetaNodeDTO candidata) {
        return fingerprint(atual).compareTo(fingerprint(candidata));
    }

    private static String fingerprint(final ColetaNodeDTO coleta) {
        if (coleta == null) {
            return "";
        }
        final Map<String, String> extras = new java.util.TreeMap<>();
        coleta.getOtherProperties().forEach((chave, valor) -> extras.put(chave, Objects.toString(valor, "")));
        return String.join("|",
            Objects.toString(coleta.getId(), ""),
            Objects.toString(coleta.getSequenceCode(), ""),
            Objects.toString(coleta.getStatusUpdatedAt(), ""),
            Objects.toString(coleta.getFinishDate(), ""),
            Objects.toString(coleta.getServiceDate(), ""),
            Objects.toString(coleta.getRequestDate(), ""),
            Objects.toString(coleta.getStatus(), ""),
            Objects.toString(coleta.getRequester(), ""),
            extras.toString()
        );
    }
}

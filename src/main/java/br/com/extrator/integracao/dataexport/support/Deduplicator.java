/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/runners/dataexport/support/Deduplicator.java
Classe  : Deduplicator (class)
Pacote  : br.com.extrator.integracao.dataexport.support
Modulo  : Servico de execucao DataExport
Papel   : Implementa responsabilidade de deduplicator.

Conecta com:
- ContasAPagarDataExportEntity (db.entity)
- CotacaoEntity (db.entity)
- FaturaPorClienteEntity (db.entity)
- LocalizacaoCargaEntity (db.entity)
- ManifestoEntity (db.entity)

Fluxo geral:
1) Coordena extractors da API DataExport.
2) Aplica deduplicacao/normalizacao quando necessario.
3) Encaminha resultado consolidado para o runner.

Estrutura interna:
Metodos principais:
- Deduplicator(): realiza operacao relacionada a "deduplicator".
- deduplicarManifestos(...1 args): realiza operacao relacionada a "deduplicar manifestos".
- obterMaisRecenteManifesto(...2 args): recupera dados configurados ou calculados.
- deduplicarCotacoes(...1 args): realiza operacao relacionada a "deduplicar cotacoes".
- obterMaisRecenteCotacao(...2 args): recupera dados configurados ou calculados.
- deduplicarLocalizacoes(...1 args): realiza operacao relacionada a "deduplicar localizacoes".
- obterMaisRecenteLocalizacao(...2 args): recupera dados configurados ou calculados.
- deduplicarFaturasAPagar(...1 args): realiza operacao relacionada a "deduplicar faturas apagar".
- deduplicarFaturasPorCliente(...1 args): realiza operacao relacionada a "deduplicar faturas por cliente".
Atributos-chave:
- logger: logger da classe para diagnostico.
[DOC-FILE-END]============================================================== */

package br.com.extrator.integracao.dataexport.support;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.persistencia.entidade.ContasAPagarDataExportEntity;
import br.com.extrator.persistencia.entidade.CotacaoEntity;
import br.com.extrator.persistencia.entidade.FaturaPorClienteEntity;
import br.com.extrator.persistencia.entidade.LocalizacaoCargaEntity;
import br.com.extrator.persistencia.entidade.ManifestoEntity;

/**
 * Classe utilitária para deduplicação de entidades do DataExport.
 * Remove registros duplicados da resposta da API antes de salvar no banco.
 * 
 * Estratégia: mantém o registro mais fresco e consolida métricas de duplicados
 * para preservar campos financeiros/operacionais que a API pode devolver
 * apenas em uma das variantes do mesmo manifesto.
 * Chaves de deduplicação são alinhadas com as chaves do MERGE SQL.
 */
public final class Deduplicator {
    private static final Logger logger = LoggerFactory.getLogger(Deduplicator.class);
    
    private Deduplicator() {}
    
    /**
     * Deduplica lista de manifestos removendo registros duplicados da API.
     * 
     * ⚠️ CRÍTICO: Usa a MESMA chave composta do MERGE SQL:
     * (sequence_code, pick_sequence_code ou identificador_unico, mdfe_number)
     * 
     * Estratégia: mantém o registro mais recente baseado em finishedAt/createdAt
     * e completa métricas ausentes com valores reais vindos do duplicado.
     * 
     * @param manifestos Lista de manifestos a deduplicar
     * @return Lista deduplicada de manifestos
     */
    public static List<ManifestoEntity> deduplicarManifestos(final List<ManifestoEntity> manifestos) {
        if (manifestos == null || manifestos.isEmpty()) {
            return manifestos;
        }
        
        return manifestos.stream()
            .collect(Collectors.toMap(
                m -> {
                    // Chave alinhada com MERGE SQL: fallback em identificador_unico quando pick e nulo.
                    if (m.getSequenceCode() == null) {
                        throw new IllegalStateException("Manifesto com sequence_code NULL não pode ser deduplicado");
                    }
                    final String pickOuIdentificador = chavePickOuIdentificador(m);
                    final Integer mdfe = m.getMdfeNumber() != null ? m.getMdfeNumber() : -1;
                    return m.getSequenceCode() + "_" + pickOuIdentificador + "_" + mdfe;
                },
                m -> m,
                (primeiro, segundo) -> {
                    final ManifestoEntity consolidado = obterMaisRecenteManifesto(primeiro, segundo);
                    logger.warn("⚠️ Duplicado detectado na API: sequence_code={}, pick={}, mdfe={}. Mantendo o mais fresco e consolidando métricas financeiras/operacionais.", 
                        primeiro.getSequenceCode(), 
                        primeiro.getPickSequenceCode(), 
                        primeiro.getMdfeNumber());
                    return consolidado;
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }

    private static String chavePickOuIdentificador(final ManifestoEntity manifesto) {
        if (manifesto.getPickSequenceCode() != null) {
            return String.valueOf(manifesto.getPickSequenceCode());
        }
        if (manifesto.getIdentificadorUnico() != null && !manifesto.getIdentificadorUnico().isBlank()) {
            return manifesto.getIdentificadorUnico();
        }
        return "-1";
    }
    
    /**
     * Determina qual manifesto é mais recente baseado em finishedAt ou createdAt
     * e preserva métricas reais que possam existir apenas em uma variante duplicada
     * do payload da ESL.
     */
    private static ManifestoEntity obterMaisRecenteManifesto(final ManifestoEntity primeiro, final ManifestoEntity segundo) {
        final int comparacaoFreshness = compararFreshnessManifesto(primeiro, segundo);
        final ManifestoEntity base;
        final ManifestoEntity complemento;

        if (comparacaoFreshness > 0) {
            base = primeiro;
            complemento = segundo;
        } else if (comparacaoFreshness < 0) {
            base = segundo;
            complemento = primeiro;
        } else if (pontuarMetricasManifesto(primeiro) >= pontuarMetricasManifesto(segundo)) {
            base = primeiro;
            complemento = segundo;
        } else {
            base = segundo;
            complemento = primeiro;
        }

        enriquecerMetricasManifesto(base, complemento);
        return base;
    }

    private static int compararFreshnessManifesto(final ManifestoEntity primeiro, final ManifestoEntity segundo) {
        final int finishedAt = comparar(primeiro.getFinishedAt(), segundo.getFinishedAt());
        if (finishedAt != 0) {
            return finishedAt;
        }

        return comparar(primeiro.getCreatedAt(), segundo.getCreatedAt());
    }

    private static int pontuarMetricasManifesto(final ManifestoEntity manifesto) {
        int pontuacao = 0;
        pontuacao += temValor(manifesto.getKm()) ? 1 : 0;
        pontuacao += temValor(manifesto.getTotalCost()) ? 1 : 0;
        pontuacao += temValor(manifesto.getManifestFreightsTotal()) ? 1 : 0;
        pontuacao += temValor(manifesto.getTotalTaxedWeight()) ? 1 : 0;
        pontuacao += temValor(manifesto.getCapacidadeKg()) ? 1 : 0;
        pontuacao += temValor(manifesto.getVehicleWeightCapacity()) ? 1 : 0;
        pontuacao += temValor(manifesto.getManifestItemsCount()) ? 1 : 0;
        pontuacao += temValor(manifesto.getFinalizedManifestItemsCount()) ? 1 : 0;
        return pontuacao;
    }

    private static void enriquecerMetricasManifesto(final ManifestoEntity base, final ManifestoEntity complemento) {
        if (base == null || complemento == null) {
            return;
        }

        base.setKm(escolherValor(base.getKm(), complemento.getKm()));
        base.setTotalCost(escolherValor(base.getTotalCost(), complemento.getTotalCost()));
        base.setManifestFreightsTotal(escolherValor(base.getManifestFreightsTotal(), complemento.getManifestFreightsTotal()));
        base.setTotalTaxedWeight(escolherValor(base.getTotalTaxedWeight(), complemento.getTotalTaxedWeight()));
        base.setCapacidadeKg(escolherValor(base.getCapacidadeKg(), complemento.getCapacidadeKg()));
        base.setVehicleWeightCapacity(escolherValor(base.getVehicleWeightCapacity(), complemento.getVehicleWeightCapacity()));
        base.setManifestItemsCount(escolherValor(base.getManifestItemsCount(), complemento.getManifestItemsCount()));
        base.setFinalizedManifestItemsCount(escolherValor(base.getFinalizedManifestItemsCount(), complemento.getFinalizedManifestItemsCount()));
    }

    private static BigDecimal escolherValor(final BigDecimal atual, final BigDecimal candidato) {
        return !temValor(atual) && temValor(candidato) ? candidato : atual;
    }

    private static Integer escolherValor(final Integer atual, final Integer candidato) {
        return !temValor(atual) && temValor(candidato) ? candidato : atual;
    }

    private static boolean temValor(final BigDecimal valor) {
        return valor != null && BigDecimal.ZERO.compareTo(valor) != 0;
    }

    private static boolean temValor(final Integer valor) {
        return valor != null && valor.intValue() != 0;
    }
    
    /**
     * Deduplica lista de cotações removendo registros duplicados da API.
     * Usa sequence_code como chave única (PRIMARY KEY da tabela).
     * 
     * Estratégia: "Keep Last" - mantém o registro mais recente baseado em requestedAt.
     * 
     * @param cotacoes Lista de cotações a deduplicar
     * @return Lista deduplicada de cotações
     */
    public static List<CotacaoEntity> deduplicarCotacoes(final List<CotacaoEntity> cotacoes) {
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
                    // ✅ ESTRATÉGIA "KEEP LAST": Manter o registro mais recente
                    final CotacaoEntity maisRecente = obterMaisRecenteCotacao(primeiro, segundo);
                    logger.warn("⚠️ Duplicado detectado na API: sequence_code={}. Mantendo o mais recente (requestedAt).", 
                        segundo.getSequenceCode());
                    return maisRecente;
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Determina qual cotação é mais recente baseado em requestedAt.
     */
    private static CotacaoEntity obterMaisRecenteCotacao(final CotacaoEntity primeiro, final CotacaoEntity segundo) {
        final OffsetDateTime requestedAt1 = primeiro.getRequestedAt();
        final OffsetDateTime requestedAt2 = segundo.getRequestedAt();
        if (requestedAt1 != null && requestedAt2 != null) {
            return requestedAt1.isAfter(requestedAt2) ? primeiro : segundo;
        }
        if (requestedAt2 != null) {
            return segundo;
        }
        if (requestedAt1 != null) {
            return primeiro;
        }
        // Fallback: manter o segundo (último processado)
        return segundo;
    }
    
    /**
     * Deduplica lista de localizações removendo registros duplicados da API.
     * Usa sequence_number como chave única (PRIMARY KEY da tabela).
     * 
     * Estratégia: "Keep Last" - mantém o registro mais recente baseado em serviceAt.
     * 
     * @param localizacoes Lista de localizações a deduplicar
     * @return Lista deduplicada de localizações
     */
    public static List<LocalizacaoCargaEntity> deduplicarLocalizacoes(final List<LocalizacaoCargaEntity> localizacoes) {
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
                    // ✅ ESTRATÉGIA "KEEP LAST": Manter o registro mais recente
                    final LocalizacaoCargaEntity maisRecente = obterMaisRecenteLocalizacao(primeiro, segundo);
                    logger.warn("⚠️ Duplicado detectado na API: sequence_number={}. Mantendo o mais recente (serviceAt).", 
                        segundo.getSequenceNumber());
                    return maisRecente;
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Determina qual localização é mais recente baseado em serviceAt.
     */
    private static LocalizacaoCargaEntity obterMaisRecenteLocalizacao(
            final LocalizacaoCargaEntity primeiro, 
            final LocalizacaoCargaEntity segundo) {
        final OffsetDateTime serviceAt1 = primeiro.getServiceAt();
        final OffsetDateTime serviceAt2 = segundo.getServiceAt();
        if (serviceAt1 != null && serviceAt2 != null) {
            return serviceAt1.isAfter(serviceAt2) ? primeiro : segundo;
        }
        if (serviceAt2 != null) {
            return segundo;
        }
        if (serviceAt1 != null) {
            return primeiro;
        }
        // Fallback: manter o segundo (último processado)
        return segundo;
    }

    /**
     * Deduplica Faturas a Pagar por sequence_code (chave primária).
     * 
     * Estratégia: "Keep Last" - mantém o último processado (sem timestamp específico).
     */
    public static List<ContasAPagarDataExportEntity> deduplicarFaturasAPagar(final List<ContasAPagarDataExportEntity> lista) {
        if (lista == null || lista.isEmpty()) {
            return lista;
        }

        return lista.stream()
            .collect(Collectors.toMap(
                e -> {
                    if (e.getSequenceCode() == null) {
                        throw new IllegalStateException("Fatura a pagar com sequence_code NULL não pode ser deduplicada");
                    }
                    return e.getSequenceCode();
                },
                e -> e,
                (primeiro, segundo) -> {
                    final ContasAPagarDataExportEntity maisFresco = obterMaisRecenteContaAPagar(primeiro, segundo);
                    logger.warn("⚠️ Duplicado detectado: sequence_code={}. Mantendo o registro mais fresco (dataCriacao/dataTransacao/dataLiquidacao).", 
                        segundo.getSequenceCode());
                    return maisFresco;
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }

    /**
     * Deduplica Faturas por Cliente por unique_id (chave primária).
     * 
     * Estratégia: "Keep Last" - mantém o último processado (sem timestamp específico).
     */
    public static List<FaturaPorClienteEntity> deduplicarFaturasPorCliente(final List<FaturaPorClienteEntity> lista) {
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
                    // ✅ ESTRATÉGIA "KEEP LAST": Manter o segundo (último processado)
                    logger.warn("⚠️ Duplicado detectado: unique_id={}. Mantendo o último processado.", 
                        segundo.getUniqueId());
                    return segundo;
                }
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
    }

    private static ContasAPagarDataExportEntity obterMaisRecenteContaAPagar(
        final ContasAPagarDataExportEntity primeiro,
        final ContasAPagarDataExportEntity segundo
    ) {
        final int dataCriacao = comparar(primeiro.getDataCriacao(), segundo.getDataCriacao());
        if (dataCriacao != 0) {
            return dataCriacao >= 0 ? primeiro : segundo;
        }

        final int dataTransacao = comparar(primeiro.getDataTransacao(), segundo.getDataTransacao());
        if (dataTransacao != 0) {
            return dataTransacao >= 0 ? primeiro : segundo;
        }

        final int dataLiquidacao = comparar(primeiro.getDataLiquidacao(), segundo.getDataLiquidacao());
        if (dataLiquidacao != 0) {
            return dataLiquidacao >= 0 ? primeiro : segundo;
        }

        final int issueDate = comparar(primeiro.getIssueDate(), segundo.getIssueDate());
        if (issueDate != 0) {
            return issueDate >= 0 ? primeiro : segundo;
        }

        final int dataExtracao = comparar(primeiro.getDataExtracao(), segundo.getDataExtracao());
        if (dataExtracao != 0) {
            return dataExtracao >= 0 ? primeiro : segundo;
        }

        return segundo;
    }

    private static <T extends Comparable<? super T>> int comparar(final T esquerda, final T direita) {
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
}

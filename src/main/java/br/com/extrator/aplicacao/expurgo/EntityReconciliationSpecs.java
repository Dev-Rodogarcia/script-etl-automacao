package br.com.extrator.aplicacao.expurgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.extrator.integracao.DataExportReconciliationKeyExtractors;
import br.com.extrator.integracao.constantes.ConstantesApiDataExport;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public final class EntityReconciliationSpecs {
    private static final Map<String, EntityReconciliationSpec> DATAEXPORT_SPECS = criarSpecsDataExport();
    private static final String TEMPORAL_FATURAS_POR_CLIENTE = """
        COALESCE(
            TRY_CONVERT(date, CASE WHEN ISJSON(base.metadata) = 1 THEN JSON_VALUE(base.metadata, '$.service_at') END),
            TRY_CONVERT(date, CASE WHEN ISJSON(base.metadata) = 1 THEN JSON_VALUE(base.metadata, '$.service_date') END),
            (
                SELECT TOP (1) COALESCE(f.service_date, CONVERT(date, f.servico_em))
                  FROM dbo.fretes f
                 WHERE f.chave_cte = base.chave_cte
                   AND COALESCE(f.excluido_na_origem, 0) = 0
                 ORDER BY f.data_extracao DESC
            ),
            TRY_CONVERT(date, base.data_emissao_cte),
            TRY_CONVERT(date, base.data_emissao_fatura),
            TRY_CONVERT(date, base.fit_ant_issue_date),
            CONVERT(date, base.data_extracao)
        )
        """;
    private static final String TEMPORAL_MANIFESTOS = """
        COALESCE(
            TRY_CONVERT(date, CASE WHEN ISJSON(base.metadata) = 1 THEN JSON_VALUE(base.metadata, '$.service_date') END),
            TRY_CONVERT(date, base.created_at),
            TRY_CONVERT(date, base.departured_at),
            TRY_CONVERT(date, base.closed_at),
            TRY_CONVERT(date, base.finished_at),
            CONVERT(date, base.data_extracao)
        )
        """;

    private EntityReconciliationSpecs() {
    }

    public static List<EntityReconciliationSpec> dataExportPadrao() {
        return List.copyOf(DATAEXPORT_SPECS.values());
    }

    public static List<EntityReconciliationSpec> resolverDataExport(final Collection<String> nomesEntidades) {
        if (nomesEntidades == null || nomesEntidades.isEmpty()) {
            return dataExportPadrao();
        }

        final List<EntityReconciliationSpec> specs = new ArrayList<>();
        for (final String entidade : nomesEntidades) {
            final String chave = normalizar(entidade);
            final EntityReconciliationSpec spec = DATAEXPORT_SPECS.get(chave);
            if (spec == null) {
                throw new IllegalArgumentException(
                    "Entidade nao suportada no expurgo DataExport inicial: " + entidade
                        + ". Suportadas: " + DATAEXPORT_SPECS.keySet()
                );
            }
            specs.add(spec);
        }
        return List.copyOf(specs);
    }

    private static Map<String, EntityReconciliationSpec> criarSpecsDataExport() {
        final Map<String, EntityReconciliationSpec> specs = new LinkedHashMap<>();
        specs.put(
            ConstantesEntidades.FATURAS_POR_CLIENTE,
            new EntityReconciliationSpec(
                ConstantesEntidades.FATURAS_POR_CLIENTE,
                "dbo.faturas_por_cliente",
                "unique_id",
                TEMPORAL_FATURAS_POR_CLIENTE,
                ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.FATURAS_POR_CLIENTE),
                DataExportReconciliationKeyExtractors::faturaPorCliente
            )
        );
        specs.put(
            ConstantesEntidades.MANIFESTOS,
            new EntityReconciliationSpec(
                ConstantesEntidades.MANIFESTOS,
                "dbo.manifestos",
                "chave_merge_hash",
                TEMPORAL_MANIFESTOS,
                ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.MANIFESTOS),
                DataExportReconciliationKeyExtractors::manifesto
            )
        );
        return Collections.unmodifiableMap(specs);
    }

    private static String normalizar(final String entidade) {
        if (entidade == null || entidade.isBlank()) {
            return "";
        }
        return entidade.trim().toLowerCase(Locale.ROOT);
    }
}

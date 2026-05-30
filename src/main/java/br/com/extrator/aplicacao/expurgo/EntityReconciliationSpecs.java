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
                ConstantesApiDataExport.obterConfiguracao(ConstantesEntidades.FATURAS_POR_CLIENTE),
                DataExportReconciliationKeyExtractors::faturaPorCliente
            )
        );
        specs.put(
            ConstantesEntidades.MANIFESTOS,
            new EntityReconciliationSpec(
                ConstantesEntidades.MANIFESTOS,
                "dbo.manifestos",
                "CONCAT(CAST(sequence_code AS varchar(50)), '|', "
                    + "COALESCE(CAST(pick_sequence_code AS varchar(50)), '-1'), '|', "
                    + "COALESCE(CAST(mdfe_number AS varchar(50)), '-1'))",
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

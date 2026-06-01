package br.com.extrator.aplicacao.expurgo;

import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.extrator.integracao.constantes.ConstantesApiDataExport.ConfiguracaoEntidade;

public record EntityReconciliationSpec(
    String entityName,
    String tableName,
    String dbKeyExpression,
    String dbTemporalExpression,
    ConfiguracaoEntidade dataExportConfig,
    Function<JsonNode, String> sourceKeyExtractor
) {
    public EntityReconciliationSpec {
        entityName = requireText(entityName, "entityName");
        tableName = requireText(tableName, "tableName");
        dbKeyExpression = requireText(dbKeyExpression, "dbKeyExpression");
        dbTemporalExpression = requireText(dbTemporalExpression, "dbTemporalExpression");
        dataExportConfig = Objects.requireNonNull(dataExportConfig, "dataExportConfig");
        sourceKeyExtractor = Objects.requireNonNull(sourceKeyExtractor, "sourceKeyExtractor");
    }

    private static String requireText(final String value, final String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " nao pode ser vazio");
        }
        return value.trim();
    }
}

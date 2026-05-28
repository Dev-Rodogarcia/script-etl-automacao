package br.com.extrator.integracao.comum;

/**
 * Contrato composto para extractors DataExport que processam paginas em chunks.
 *
 * @param <T> Tipo do DTO retornado pela API
 */
public interface ChunkedDataExportEntityExtractor<T>
    extends DataExportEntityExtractor<T>, ChunkedEntityExtractor<T> {
}

package br.com.extrator.util.mapeamento;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import br.com.extrator.util.formatacao.FormatadorData;

/**
 * Utilitário centralizado para parsing de datas e timestamps.
 * 
 * DELEGAÇÃO: Métodos delegam para FormatadorData para evitar duplicação de lógica.
 * Mantém API simples para mappers enquanto FormatadorData tem funcionalidades completas.
 * 
 * @since 2.3.2
 * @see FormatadorData
 */
public final class DataUtil {
    
    private DataUtil() {
        // Classe utilitária - construtor privado
    }
    
    /**
     * Converte string para LocalDate (formato ISO: yyyy-MM-dd).
     * 
     * Delega para FormatadorData.parseLocalDate().
     * 
     * @param data String no formato ISO
     * @return LocalDate ou null se inválido/vazio
     */
    public static LocalDate parseLocalDate(final String data) {
        return FormatadorData.parseLocalDate(data);
    }
    
    /**
     * Converte string para OffsetDateTime (formato ISO com timezone).
     * 
     * Delega para FormatadorData.parseOffsetDateTime().
     * 
     * @param dataHora String no formato ISO com timezone
     * @return OffsetDateTime ou null se inválido/vazio
     */
    public static OffsetDateTime parseOffsetDateTime(final String dataHora) {
        return FormatadorData.parseOffsetDateTime(dataHora);
    }
}

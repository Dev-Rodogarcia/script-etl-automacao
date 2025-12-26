package br.com.extrator.util.formatacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe utilitária centralizada para formatação e parsing de datas.
 * Evita duplicação de DateTimeFormatter espalhados pelo código.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public final class FormatadorData {
    
    private static final Logger logger = LoggerFactory.getLogger(FormatadorData.class);
    
    // ========== FORMATADORES PADRÃO ==========
    
    /** Formato ISO padrão: yyyy-MM-dd */
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /** Formato ISO padrão: yyyy-MM-dd'T'HH:mm:ss */
    public static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /** Formato ISO com offset: yyyy-MM-dd'T'HH:mm:ssXXX */
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    /** Formato brasileiro: dd/MM/yyyy */
    public static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    /** Formato brasileiro com hora: dd/MM/yyyy HH:mm:ss */
    public static final DateTimeFormatter BR_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    /** Formato para logs: yyyy-MM-dd HH:mm:ss */
    public static final DateTimeFormatter LOG_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /** Formato para nomes de arquivo: yyyy-MM-dd_HH-mm-ss */
    public static final DateTimeFormatter FILE_NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    // ========== MÉTODOS DE PARSING ==========
    
    /**
     * Converte String para LocalDate de forma segura.
     * 
     * @param dateStr String no formato yyyy-MM-dd
     * @return LocalDate ou null se inválido
     */
    public static LocalDate parseLocalDate(final String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), ISO_DATE);
        } catch (final DateTimeParseException e) {
            logger.warn("Erro ao parsear LocalDate '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
    
    /**
     * Converte String para LocalDate com formatador customizado.
     * 
     * @param dateStr String de data
     * @param formatter Formatador a usar
     * @return LocalDate ou null se inválido
     */
    public static LocalDate parseLocalDate(final String dateStr, final DateTimeFormatter formatter) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), formatter);
        } catch (final DateTimeParseException e) {
            logger.warn("Erro ao parsear LocalDate '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
    
    /**
     * Converte String para LocalDateTime de forma segura.
     * 
     * @param dateTimeStr String no formato ISO
     * @return LocalDateTime ou null se inválido
     */
    public static LocalDateTime parseLocalDateTime(final String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim());
        } catch (final DateTimeParseException e) {
            logger.warn("Erro ao parsear LocalDateTime '{}': {}", dateTimeStr, e.getMessage());
            return null;
        }
    }
    
    /**
     * Converte String para OffsetDateTime de forma segura.
     * 
     * @param dateTimeStr String no formato ISO com offset
     * @return OffsetDateTime ou null se inválido
     */
    public static OffsetDateTime parseOffsetDateTime(final String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateTimeStr.trim());
        } catch (final DateTimeParseException e) {
            logger.warn("Erro ao parsear OffsetDateTime '{}': {}", dateTimeStr, e.getMessage());
            return null;
        }
    }
    
    // ========== MÉTODOS DE FORMATAÇÃO ==========
    
    /**
     * Formata LocalDate para String ISO (yyyy-MM-dd).
     * 
     * @param date Data a formatar
     * @return String formatada ou null
     */
    public static String formatISO(final LocalDate date) {
        return date != null ? date.format(ISO_DATE) : null;
    }
    
    /**
     * Formata LocalDateTime para String ISO.
     * 
     * @param dateTime Data/hora a formatar
     * @return String formatada ou null
     */
    public static String formatISO(final LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(ISO_DATE_TIME) : null;
    }
    
    /**
     * Formata LocalDate para formato brasileiro (dd/MM/yyyy).
     * 
     * @param date Data a formatar
     * @return String formatada ou null
     */
    public static String formatBR(final LocalDate date) {
        return date != null ? date.format(BR_DATE) : null;
    }
    
    /**
     * Formata LocalDateTime para formato brasileiro (dd/MM/yyyy HH:mm:ss).
     * 
     * @param dateTime Data/hora a formatar
     * @return String formatada ou null
     */
    public static String formatBR(final LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(BR_DATE_TIME) : null;
    }
    
    /**
     * Formata LocalDateTime para nome de arquivo (yyyy-MM-dd_HH-mm-ss).
     * 
     * @param dateTime Data/hora a formatar
     * @return String formatada ou null
     */
    public static String formatFileName(final LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(FILE_NAME) : null;
    }
    
    /**
     * Formata LocalDateTime para logs (yyyy-MM-dd HH:mm:ss).
     * 
     * @param dateTime Data/hora a formatar
     * @return String formatada ou null
     */
    public static String formatLog(final LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(LOG_DATE_TIME) : null;
    }
    
    /**
     * Retorna a data/hora atual formatada para logs.
     * 
     * @return String formatada
     */
    public static String agoraFormatadoLog() {
        return LocalDateTime.now().format(LOG_DATE_TIME);
    }
    
    /**
     * Retorna a data/hora atual formatada para nome de arquivo.
     * 
     * @return String formatada
     */
    public static String agoraFormatadoArquivo() {
        return LocalDateTime.now().format(FILE_NAME);
    }
    
    private FormatadorData() {
        // Impede instanciação
    }
}

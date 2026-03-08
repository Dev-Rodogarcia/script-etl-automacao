package br.com.extrator.aplicacao.politicas;

public enum ErrorTaxonomy {
    TRANSIENT_API_ERROR,
    PERMANENT_VALIDATION_ERROR,
    DB_CONFLICT,
    DATA_QUALITY_BREACH,
    TIMEOUT,
    SCHEMA_DRIFT
}


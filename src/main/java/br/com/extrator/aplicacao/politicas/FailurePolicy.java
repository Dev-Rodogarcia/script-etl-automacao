package br.com.extrator.aplicacao.politicas;

public interface FailurePolicy {
    FailureMode resolver(String entidade, ErrorTaxonomy taxonomy);
}



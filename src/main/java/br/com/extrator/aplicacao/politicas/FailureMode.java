package br.com.extrator.aplicacao.politicas;

public enum FailureMode {
    ABORT_PIPELINE,
    CONTINUE_WITH_ALERT,
    RETRY,
    DEGRADE
}


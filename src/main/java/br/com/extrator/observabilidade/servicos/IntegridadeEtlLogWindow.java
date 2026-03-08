package br.com.extrator.observabilidade.servicos;

import java.time.LocalDateTime;

record IntegridadeEtlLogWindow(
    String status,
    int registrosExtraidos,
    LocalDateTime inicio,
    LocalDateTime fim,
    String mensagem
) {
}

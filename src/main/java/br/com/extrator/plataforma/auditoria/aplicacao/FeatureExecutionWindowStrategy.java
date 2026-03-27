package br.com.extrator.plataforma.auditoria.aplicacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import br.com.extrator.plataforma.auditoria.dominio.ExecutionWindowPlan;

public interface FeatureExecutionWindowStrategy {

    String entidade();

    ExecutionWindowPlan planejar(LocalDate dataReferenciaFim, Optional<LocalDateTime> watermarkConfirmado);
}

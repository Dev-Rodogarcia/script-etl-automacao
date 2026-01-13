package br.com.extrator.servicos;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.repository.LogExtracaoRepository;
import br.com.extrator.util.console.LoggerConsole;

/**
 * Serviço responsável por validar regras de limitação de tempo para extrações
 * baseadas no período consultado.
 * 
 * Regras:
 * - < 31 dias: sem limite de tempo
 * - 31 dias a 6 meses: mínimo 1 hora desde última extração do mesmo período
 * - > 6 meses: mínimo 12 horas desde última extração do mesmo período
 */
public class ValidadorLimiteExtracao {
    
    private static final LoggerConsole log = LoggerConsole.getLogger(ValidadorLimiteExtracao.class);
    
    private static final int DIAS_31 = 31;
    private static final int DIAS_6_MESES = 180; // Aproximadamente 6 meses
    
    private final LogExtracaoRepository logRepository;
    
    public ValidadorLimiteExtracao() {
        this.logRepository = new LogExtracaoRepository();
    }
    
    /**
     * Resultado da validação de limite de extração
     */
    public static class ResultadoValidacao {
        private final boolean permitido;
        private final String motivo;
        private final long horasRestantes;
        private final int limiteHoras;
        
        private ResultadoValidacao(final boolean permitido, final String motivo, final long horasRestantes, final int limiteHoras) {
            this.permitido = permitido;
            this.motivo = motivo;
            this.horasRestantes = horasRestantes;
            this.limiteHoras = limiteHoras;
        }
        
        public static ResultadoValidacao permitido() {
            return new ResultadoValidacao(true, "Extração permitida", 0, 0);
        }
        
        public static ResultadoValidacao bloqueado(final String motivo, final long horasRestantes, final int limiteHoras) {
            return new ResultadoValidacao(false, motivo, horasRestantes, limiteHoras);
        }
        
        public boolean isPermitido() {
            return permitido;
        }
        
        public String getMotivo() {
            return motivo;
        }
        
        public long getHorasRestantes() {
            return horasRestantes;
        }
        
        public int getLimiteHoras() {
            return limiteHoras;
        }
    }
    
    /**
     * Valida se é permitido executar uma extração para o período especificado
     * baseado nas regras de limitação de tempo.
     * 
     * @param entidade Nome da entidade a ser extraída
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return ResultadoValidacao indicando se é permitido ou bloqueado
     */
    public ResultadoValidacao validarLimiteExtracao(final String entidade, 
                                                     final LocalDate dataInicio, 
                                                     final LocalDate dataFim) {
        // Calcular duração do período em dias
        final long diasPeriodo = calcularDuracaoPeriodo(dataInicio, dataFim);
        
        // Obter limite de horas baseado na duração
        final int limiteHoras = obterLimiteHoras(diasPeriodo);
        
        // Se não há limite (período < 31 dias), permitir imediatamente
        if (limiteHoras == 0) {
            log.debug("Período de {} dias (< 31 dias) - sem limite de tempo", diasPeriodo);
            return ResultadoValidacao.permitido();
        }
        
        // Buscar última extração do mesmo período
        final Optional<LogExtracaoEntity> ultimaExtracao = 
            logRepository.buscarUltimaExtracaoPorPeriodo(entidade, dataInicio, dataFim);
        
        // Se não há extração anterior, permitir
        if (ultimaExtracao.isEmpty()) {
            log.debug("Nenhuma extração anterior encontrada para período {} a {} - permitindo", 
                     dataInicio, dataFim);
            return ResultadoValidacao.permitido();
        }
        
        // Calcular tempo decorrido desde última extração
        final LogExtracaoEntity logExtracao = ultimaExtracao.get();
        final LocalDateTime agora = LocalDateTime.now();
        final LocalDateTime ultimaExtracaoFim = logExtracao.getTimestampFim();
        
        final Duration tempoDecorrido = Duration.between(ultimaExtracaoFim, agora);
        final long horasDecorridas = tempoDecorrido.toHours();
        final long minutosRestantes = tempoDecorrido.toMinutes() % 60;
        
        // Verificar se já passou o tempo mínimo
        if (horasDecorridas >= limiteHoras) {
            log.info("✅ Limite de {} horas já foi atingido (decorridas: {}h {}min) - permitindo extração", 
                    limiteHoras, horasDecorridas, minutosRestantes);
            return ResultadoValidacao.permitido();
        }
        
        // Calcular tempo restante
        final long horasRestantes = limiteHoras - horasDecorridas;
        final long minutosRestantesTotal = (limiteHoras * 60) - tempoDecorrido.toMinutes();
        
        final String motivo = String.format(
            "Extração bloqueada: necessário aguardar %d hora(s) desde última extração (decorridas: %dh %dmin, restam: %dh %dmin)",
            limiteHoras, horasDecorridas, minutosRestantes, horasRestantes, minutosRestantesTotal % 60
        );
        
        log.warn("⏳ {}", motivo);
        
        return ResultadoValidacao.bloqueado(motivo, horasRestantes, limiteHoras);
    }
    
    /**
     * Valida se é permitido executar uma extração para o período especificado,
     * usando o período TOTAL solicitado para determinar a regra de limitação.
     * 
     * Esta variante é usada quando o período é dividido em blocos, garantindo que
     * a regra correta seja aplicada baseada no período completo, não no tamanho do bloco.
     * 
     * @param entidade Nome da entidade a ser extraída
     * @param dataInicio Data de início do bloco
     * @param dataFim Data de fim do bloco
     * @param diasPeriodoTotal Número total de dias do período completo
     * @return ResultadoValidacao indicando se é permitido ou bloqueado
     */
    public ResultadoValidacao validarLimiteExtracaoPorPeriodoTotal(final String entidade, 
                                                                    final LocalDate dataInicio, 
                                                                    final LocalDate dataFim,
                                                                    final long diasPeriodoTotal) {
        // Obter limite de horas baseado no período TOTAL (não do bloco)
        final int limiteHoras = obterLimiteHoras(diasPeriodoTotal);
        
        // Se não há limite (período < 31 dias), permitir imediatamente
        if (limiteHoras == 0) {
            log.debug("Período total de {} dias (< 31 dias) - sem limite de tempo", diasPeriodoTotal);
            return ResultadoValidacao.permitido();
        }
        
        // Buscar última extração do mesmo período (do bloco)
        final Optional<LogExtracaoEntity> ultimaExtracao = 
            logRepository.buscarUltimaExtracaoPorPeriodo(entidade, dataInicio, dataFim);
        
        // Se não há extração anterior, permitir
        if (ultimaExtracao.isEmpty()) {
            log.debug("Nenhuma extração anterior encontrada para período {} a {} - permitindo", 
                     dataInicio, dataFim);
            return ResultadoValidacao.permitido();
        }
        
        // Calcular tempo decorrido desde última extração
        final LogExtracaoEntity logExtracao = ultimaExtracao.get();
        final LocalDateTime agora = LocalDateTime.now();
        final LocalDateTime ultimaExtracaoFim = logExtracao.getTimestampFim();
        
        final Duration tempoDecorrido = Duration.between(ultimaExtracaoFim, agora);
        final long horasDecorridas = tempoDecorrido.toHours();
        final long minutosRestantes = tempoDecorrido.toMinutes() % 60;
        
        // Verificar se já passou o tempo mínimo
        if (horasDecorridas >= limiteHoras) {
            log.info("✅ Limite de {} horas já foi atingido (decorridas: {}h {}min) - permitindo extração", 
                    limiteHoras, horasDecorridas, minutosRestantes);
            return ResultadoValidacao.permitido();
        }
        
        // Calcular tempo restante
        final long horasRestantes = limiteHoras - horasDecorridas;
        final long minutosRestantesTotal = (limiteHoras * 60) - tempoDecorrido.toMinutes();
        
        final String motivo = String.format(
            "Extração bloqueada: necessário aguardar %d hora(s) desde última extração (decorridas: %dh %dmin, restam: %dh %dmin)",
            limiteHoras, horasDecorridas, minutosRestantes, horasRestantes, minutosRestantesTotal % 60
        );
        
        log.warn("⏳ {}", motivo);
        
        return ResultadoValidacao.bloqueado(motivo, horasRestantes, limiteHoras);
    }
    
    /**
     * Calcula a duração do período em dias (inclusive).
     * 
     * @param inicio Data de início
     * @param fim Data de fim
     * @return Número de dias do período
     */
    public long calcularDuracaoPeriodo(final LocalDate inicio, final LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException(
                String.format("Data de início (%s) não pode ser posterior à data de fim (%s)", inicio, fim)
            );
        }
        
        // ChronoUnit.DAYS.between retorna dias entre as datas (exclusive)
        // Adicionamos 1 para incluir ambas as datas
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }
    
    /**
     * Obtém o limite de horas baseado na duração do período.
     * 
     * @param diasPeriodo Duração do período em dias
     * @return Limite de horas: 0 (sem limite), 1 (1 hora) ou 12 (12 horas)
     */
    public int obterLimiteHoras(final long diasPeriodo) {
        if (diasPeriodo < DIAS_31) {
            return 0; // Sem limite
        } else if (diasPeriodo <= DIAS_6_MESES) {
            return 1; // 1 hora
        } else {
            return 12; // 12 horas
        }
    }
}


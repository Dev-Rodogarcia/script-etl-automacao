package br.com.extrator.runners.common;

import java.time.LocalDateTime;

import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Wrapper para resultados de extraÃ§Ã£o, facilitando logging e tratamento de erros.
 */
public class ExtractionResult {
    private final String entityName;
    private final LocalDateTime inicio;
    private final LocalDateTime fim;
    private final String status;
    private final int registrosSalvos;
    private final int registrosExtraidos; // Total extraÃ­do da API (antes de deduplicaÃ§Ã£o)
    private final int totalUnicos; // Total apÃ³s deduplicaÃ§Ã£o (para DataExport)
    private final int paginasProcessadas;
    private final String mensagem;
    private final boolean sucesso;
    private final Exception erro;
    
    private ExtractionResult(final Builder builder) {
        this.entityName = builder.entityName;
        this.inicio = builder.inicio;
        this.fim = builder.fim;
        this.status = builder.status;
        this.registrosSalvos = builder.registrosSalvos;
        this.registrosExtraidos = builder.registrosExtraidos;
        this.totalUnicos = builder.totalUnicos;
        this.paginasProcessadas = builder.paginasProcessadas;
        this.mensagem = builder.mensagem;
        this.sucesso = builder.sucesso;
        this.erro = builder.erro;
    }
    
    public static Builder sucesso(final String entityName, 
                                  final LocalDateTime inicio,
                                  final ResultadoExtracao<?> resultado,
                                  final int registrosSalvos,
                                  final String mensagem) {
        return new Builder(entityName, inicio)
            .status(resultado.isCompleto() ? ConstantesEntidades.STATUS_COMPLETO : ConstantesEntidades.STATUS_INCOMPLETO_LIMITE)
            .registrosSalvos(registrosSalvos)
            .registrosExtraidos(resultado.getRegistrosExtraidos())
            .totalUnicos(resultado.getDados().size()) // PadrÃ£o: tamanho da lista
            .paginasProcessadas(resultado.getPaginasProcessadas())
            .mensagem(mensagem)
            .sucesso(true);
    }
    
    public static Builder sucessoComUnicos(final String entityName,
                                           final LocalDateTime inicio,
                                           final ResultadoExtracao<?> resultado,
                                           final int registrosSalvos,
                                           final int totalUnicos,
                                           final String mensagem) {
        return new Builder(entityName, inicio)
            .status(resultado.isCompleto() ? ConstantesEntidades.STATUS_COMPLETO : ConstantesEntidades.STATUS_INCOMPLETO_LIMITE)
            .registrosSalvos(registrosSalvos)
            .registrosExtraidos(resultado.getRegistrosExtraidos())
            .totalUnicos(totalUnicos)
            .paginasProcessadas(resultado.getPaginasProcessadas())
            .mensagem(mensagem)
            .sucesso(true);
    }
    
    public static Builder erro(final String entityName,
                               final LocalDateTime inicio,
                               final Exception erro) {
        return erroComParcial(entityName, inicio, erro, 0, 0);
    }

    public static Builder erroComParcial(final String entityName,
                                         final LocalDateTime inicio,
                                         final Exception erro,
                                         final int registrosExtraidos,
                                         final int paginasProcessadas) {
        final String sufixoProgresso = registrosExtraidos > 0 || paginasProcessadas > 0
            ? String.format(" | Progresso antes da falha: %d registros da API, %d pÃ¡ginas", registrosExtraidos, paginasProcessadas)
            : "";
        return new Builder(entityName, inicio)
            .status(ConstantesEntidades.STATUS_ERRO_API)
            .registrosSalvos(0)
            .registrosExtraidos(Math.max(0, registrosExtraidos))
            .totalUnicos(0)
            .paginasProcessadas(Math.max(0, paginasProcessadas))
            .mensagem("Erro: " + erro.getMessage() + sufixoProgresso)
            .sucesso(false)
            .erro(erro);
    }
    
    public LogExtracaoEntity toLogEntity() {
        // Para DataExport, usa totalUnicos como registrosExtraidos no log
        // Para usuarios_sistema (MERGE por user_id), gravar o que foi efetivamente salvo para bater com o banco
        // Para demais GraphQL, usa registrosExtraidos diretamente
        final int registrosParaLog;
        if (ConstantesEntidades.USUARIOS_SISTEMA.equals(entityName)) {
            registrosParaLog = registrosSalvos;
        } else if (totalUnicos > 0 && totalUnicos != registrosExtraidos) {
            registrosParaLog = totalUnicos;
        } else {
            registrosParaLog = registrosExtraidos;
        }
        
        // LogExtracaoEntity aceita String no construtor e converte para enum
        return new LogExtracaoEntity(
            entityName,
            inicio,
            fim != null ? fim : LocalDateTime.now(),
            status, // String - serÃ¡ convertido para enum internamente
            registrosParaLog,
            paginasProcessadas,
            mensagem
        );
    }
    
    public String getEntityName() {
        return entityName;
    }
    
    public boolean isSucesso() {
        return sucesso;
    }
    
    public Exception getErro() {
        return erro;
    }
    
    public int getRegistrosSalvos() {
        return registrosSalvos;
    }
    
    public int getRegistrosExtraidos() {
        return registrosExtraidos;
    }
    
    public int getPaginasProcessadas() {
        return paginasProcessadas;
    }
    
    public String getStatus() {
        return status;
    }
    
    public int getTotalUnicos() {
        return totalUnicos;
    }
    
    public static class Builder {
        private final String entityName;
        private final LocalDateTime inicio;
        private LocalDateTime fim;
        private String status;
        private int registrosSalvos;
        private int registrosExtraidos;
        private int totalUnicos;
        private int paginasProcessadas;
        private String mensagem;
        private boolean sucesso;
        private Exception erro;
        
        public Builder(final String entityName, final LocalDateTime inicio) {
            this.entityName = entityName;
            this.inicio = inicio;
            this.fim = LocalDateTime.now();
        }
        
        public Builder fim(final LocalDateTime fim) {
            this.fim = fim;
            return this;
        }
        
        public Builder status(final String status) {
            this.status = status;
            return this;
        }
        
        public Builder registrosSalvos(final int registrosSalvos) {
            this.registrosSalvos = registrosSalvos;
            return this;
        }
        
        public Builder registrosExtraidos(final int registrosExtraidos) {
            this.registrosExtraidos = registrosExtraidos;
            return this;
        }
        
        public Builder totalUnicos(final int totalUnicos) {
            this.totalUnicos = totalUnicos;
            return this;
        }
        
        public Builder paginasProcessadas(final int paginasProcessadas) {
            this.paginasProcessadas = paginasProcessadas;
            return this;
        }
        
        public Builder mensagem(final String mensagem) {
            this.mensagem = mensagem;
            return this;
        }
        
        public Builder sucesso(final boolean sucesso) {
            this.sucesso = sucesso;
            return this;
        }
        
        public Builder erro(final Exception erro) {
            this.erro = erro;
            return this;
        }
        
        public ExtractionResult build() {
            return new ExtractionResult(this);
        }
    }
}

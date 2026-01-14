package br.com.extrator.db.entity;

import java.time.LocalDateTime;

/**
 * Entity (Entidade) que representa uma linha na tabela 'dim_usuarios_sistema' do banco de dados.
 * Tabela dimensão para armazenar informações de usuários do sistema (Individual).
 */
public class UsuarioSistemaEntity {

    private Long userId;
    private String nome;
    private LocalDateTime dataAtualizacao;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(final String nome) {
        this.nome = nome;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(final LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
}

package br.com.juntto.model;

import br.com.juntto.exception.AcessoNegadoException;
import br.com.juntto.exception.RegraNegocioException;

import java.time.LocalDateTime;

// Tabela tb_feedbacks: uma por serviço. Quem recebeu a ajuda avalia o ajudante (*_usu_ajudante)
// e o ajudante avalia quem recebeu a ajuda (*_usu_pedido).
public class Feedback {

    private String id;
    private String idCandidatura;
    private String idPedido;
    private String tituloPedido;
    private String idAjudante;
    private String nomeAjudante;
    private String idAjudado;
    private String nomeAjudado;
    private Integer estrelasAjudante;
    private String feedbackAjudante;
    private LocalDateTime dtAvaliacaoAjudante;
    private Integer estrelasAjudado;
    private String feedbackAjudado;
    private LocalDateTime dtAvaliacaoAjudado;

    public Feedback() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdCandidatura() { return idCandidatura; }
    public void setIdCandidatura(String idCandidatura) { this.idCandidatura = idCandidatura; }

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }

    public String getTituloPedido() { return tituloPedido; }
    public void setTituloPedido(String tituloPedido) { this.tituloPedido = tituloPedido; }

    public String getIdAjudante() { return idAjudante; }
    public void setIdAjudante(String idAjudante) { this.idAjudante = idAjudante; }

    public String getNomeAjudante() { return nomeAjudante; }
    public void setNomeAjudante(String nomeAjudante) { this.nomeAjudante = nomeAjudante; }

    public String getIdAjudado() { return idAjudado; }
    public void setIdAjudado(String idAjudado) { this.idAjudado = idAjudado; }

    public String getNomeAjudado() { return nomeAjudado; }
    public void setNomeAjudado(String nomeAjudado) { this.nomeAjudado = nomeAjudado; }

    public Integer getEstrelasAjudante() { return estrelasAjudante; }
    public void setEstrelasAjudante(Integer estrelasAjudante) { this.estrelasAjudante = estrelasAjudante; }

    public String getFeedbackAjudante() { return feedbackAjudante; }
    public void setFeedbackAjudante(String feedbackAjudante) { this.feedbackAjudante = feedbackAjudante; }

    public LocalDateTime getDtAvaliacaoAjudante() { return dtAvaliacaoAjudante; }
    public void setDtAvaliacaoAjudante(LocalDateTime dtAvaliacaoAjudante) { this.dtAvaliacaoAjudante = dtAvaliacaoAjudante; }

    public Integer getEstrelasAjudado() { return estrelasAjudado; }
    public void setEstrelasAjudado(Integer estrelasAjudado) { this.estrelasAjudado = estrelasAjudado; }

    public String getFeedbackAjudado() { return feedbackAjudado; }
    public void setFeedbackAjudado(String feedbackAjudado) { this.feedbackAjudado = feedbackAjudado; }

    public LocalDateTime getDtAvaliacaoAjudado() { return dtAvaliacaoAjudado; }
    public void setDtAvaliacaoAjudado(LocalDateTime dtAvaliacaoAjudado) { this.dtAvaliacaoAjudado = dtAvaliacaoAjudado; }

    public static final int DIAS_PARA_AVALIAR = 14;
    public static final int DIAS_PARA_EDITAR = 7;

    // Quem recebeu a ajuda avalia o ajudante (colunas *_usu_ajudante)
    public boolean autorEhAjudado(String usuarioId) {
        return idAjudado != null && idAjudado.equals(usuarioId);
    }

    // Quem ajudou avalia quem recebeu a ajuda (colunas *_usu_pedido)
    public boolean autorEhAjudante(String usuarioId) {
        return idAjudante != null && idAjudante.equals(usuarioId);
    }

    public void validarParticipante(String usuarioId) {
        if (!autorEhAjudado(usuarioId) && !autorEhAjudante(usuarioId)) {
            throw new AcessoNegadoException("Somente quem recebeu e quem deu a ajuda neste serviço podem avaliá-lo.");
        }
    }

    // Regra: cada um edita ou apaga a própria avaliação até 7 dias depois de avaliar
    public void validarPrazoDeEdicao(LocalDateTime dataDaAvaliacao) {
        if (dataDaAvaliacao == null) {
            throw new RegraNegocioException("Você ainda não avaliou este serviço.");
        }
        if (LocalDateTime.now().isAfter(dataDaAvaliacao.plusDays(DIAS_PARA_EDITAR))) {
            throw new RegraNegocioException("O prazo de " + DIAS_PARA_EDITAR + " dias para alterar esta avaliação terminou.");
        }
    }

    public boolean vazio() {
        return estrelasAjudante == null && estrelasAjudado == null;
    }

}

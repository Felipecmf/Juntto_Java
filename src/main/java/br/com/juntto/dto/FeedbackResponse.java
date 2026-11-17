package br.com.juntto.dto;

import java.time.LocalDateTime;

// [GS] Saída de feedback: avaliação do ajudante (feita por quem recebeu a ajuda) e de quem recebeu a ajuda (feita pelo ajudante)
public class FeedbackResponse {

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

    public String getId() { return id; }
    public String getIdCandidatura() { return idCandidatura; }
    public String getIdPedido() { return idPedido; }
    public String getTituloPedido() { return tituloPedido; }
    public String getIdAjudante() { return idAjudante; }
    public String getNomeAjudante() { return nomeAjudante; }
    public String getIdAjudado() { return idAjudado; }
    public String getNomeAjudado() { return nomeAjudado; }
    public Integer getEstrelasAjudante() { return estrelasAjudante; }
    public String getFeedbackAjudante() { return feedbackAjudante; }
    public LocalDateTime getDtAvaliacaoAjudante() { return dtAvaliacaoAjudante; }
    public Integer getEstrelasAjudado() { return estrelasAjudado; }
    public String getFeedbackAjudado() { return feedbackAjudado; }
    public LocalDateTime getDtAvaliacaoAjudado() { return dtAvaliacaoAjudado; }
}

package br.com.juntto.dto;

import br.com.juntto.model.StatusCandidatura;
import br.com.juntto.model.StatusPagamento;
import br.com.juntto.model.StatusPedido;

import java.time.LocalDateTime;

// [GS] Saída de candidatura: dados do candidato (com avaliações) para o solicitante escolher
public class CandidaturaResponse {

    private String id;
    private String idPedido;
    private String tituloPedido;
    private StatusPedido statusPedido;
    private String idSolicitante;
    private String idPrestador;
    private String nomePrestador;
    private String fotoPrestador;
    private Double mediaEstrelasPrestador;
    private Integer totalAvaliacoesPrestador;
    private StatusCandidatura status;
    private StatusPagamento statusPagamento;
    private LocalDateTime dtOferta;
    private LocalDateTime dtConfirmacaoPagamento;

    public String getId() { return id; }
    public String getIdPedido() { return idPedido; }
    public String getTituloPedido() { return tituloPedido; }
    public StatusPedido getStatusPedido() { return statusPedido; }
    public String getIdSolicitante() { return idSolicitante; }
    public String getIdPrestador() { return idPrestador; }
    public String getNomePrestador() { return nomePrestador; }
    public String getFotoPrestador() { return fotoPrestador; }
    public Double getMediaEstrelasPrestador() { return mediaEstrelasPrestador; }
    public Integer getTotalAvaliacoesPrestador() { return totalAvaliacoesPrestador; }
    public StatusCandidatura getStatus() { return status; }
    public StatusPagamento getStatusPagamento() { return statusPagamento; }
    public LocalDateTime getDtOferta() { return dtOferta; }
    public LocalDateTime getDtConfirmacaoPagamento() { return dtConfirmacaoPagamento; }
}

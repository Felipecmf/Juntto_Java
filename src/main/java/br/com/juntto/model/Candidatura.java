package br.com.juntto.model;

import br.com.juntto.exception.AcessoNegadoException;

import java.time.LocalDateTime;

// Tabela tb_candidatura_pedido: um prestador se oferece para um pedido. Fica no histórico mesmo depois de encerrada.
public class Candidatura {

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

    public Candidatura() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }

    public String getTituloPedido() { return tituloPedido; }
    public void setTituloPedido(String tituloPedido) { this.tituloPedido = tituloPedido; }

    public StatusPedido getStatusPedido() { return statusPedido; }
    public void setStatusPedido(StatusPedido statusPedido) { this.statusPedido = statusPedido; }

    public String getIdSolicitante() { return idSolicitante; }
    public void setIdSolicitante(String idSolicitante) { this.idSolicitante = idSolicitante; }

    public String getIdPrestador() { return idPrestador; }
    public void setIdPrestador(String idPrestador) { this.idPrestador = idPrestador; }

    public String getNomePrestador() { return nomePrestador; }
    public void setNomePrestador(String nomePrestador) { this.nomePrestador = nomePrestador; }

    public String getFotoPrestador() { return fotoPrestador; }
    public void setFotoPrestador(String fotoPrestador) { this.fotoPrestador = fotoPrestador; }

    public Double getMediaEstrelasPrestador() { return mediaEstrelasPrestador; }
    public void setMediaEstrelasPrestador(Double mediaEstrelasPrestador) { this.mediaEstrelasPrestador = mediaEstrelasPrestador; }

    public Integer getTotalAvaliacoesPrestador() { return totalAvaliacoesPrestador; }
    public void setTotalAvaliacoesPrestador(Integer totalAvaliacoesPrestador) { this.totalAvaliacoesPrestador = totalAvaliacoesPrestador; }

    public StatusCandidatura getStatus() { return status; }
    public void setStatus(StatusCandidatura status) { this.status = status; }

    public StatusPagamento getStatusPagamento() { return statusPagamento; }
    public void setStatusPagamento(StatusPagamento statusPagamento) { this.statusPagamento = statusPagamento; }

    public LocalDateTime getDtOferta() { return dtOferta; }
    public void setDtOferta(LocalDateTime dtOferta) { this.dtOferta = dtOferta; }

    public LocalDateTime getDtConfirmacaoPagamento() { return dtConfirmacaoPagamento; }
    public void setDtConfirmacaoPagamento(LocalDateTime dtConfirmacaoPagamento) { this.dtConfirmacaoPagamento = dtConfirmacaoPagamento; }

    // Ainda participa do pedido (na fila ou escolhida)
    public boolean ativa() {
        return status == StatusCandidatura.PENDENTE || status == StatusCandidatura.ESCOLHIDO;
    }

    public boolean ehDoPrestador(String usuarioId) {
        return idPrestador != null && idPrestador.equals(usuarioId);
    }

    public boolean ehDoSolicitante(String usuarioId) {
        return idSolicitante != null && idSolicitante.equals(usuarioId);
    }

    public void validarPrestador(String usuarioId) {
        if (!ehDoPrestador(usuarioId)) {
            throw new AcessoNegadoException("Somente o prestador desta candidatura pode fazer esta ação.");
        }
    }

    // Quem pode ver a candidatura: o próprio candidato ou o dono do pedido
    public void validarVisualizacao(String usuarioId) {
        if (!ehDoPrestador(usuarioId) && !ehDoSolicitante(usuarioId)) {
            throw new AcessoNegadoException("Somente o candidato ou quem fez o pedido podem ver esta candidatura.");
        }
    }

}

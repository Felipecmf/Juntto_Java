package br.com.juntto.dto;

import br.com.juntto.model.StatusPagamento;
import br.com.juntto.model.StatusPedido;

import java.time.LocalDateTime;

// [GS] Saída de pedido: dados, prestador escolhido e pagamento (valor, caixinha, total)
public class PedidoResponse {

    private String id;
    private String titulo;
    private String descricao;
    private String tipoPublicacao;
    private String idCategoria;
    private String nomeCategoria;
    private String idSolicitante;
    private String nomeSolicitante;
    private StatusPedido status;
    private LocalDateTime dtInicio;
    private LocalDateTime dtFinal;
    private Double valor;
    private Boolean naoIncluirPagamento;
    private String recompensa;
    private Double caixinha;
    private Double total;
    private String fotoUrl;
    private Double latitude;
    private Double longitude;
    private String idCandidaturaEscolhida;
    private String idPrestador;
    private String idAjudante;
    private String idAjudado;
    private String nomePrestador;
    private StatusPagamento statusPagamento;
    private LocalDateTime dtPagamento;
    private LocalDateTime dtConfirmacaoPagamento;
    private String emailSolicitante;
    private String emailPrestador;

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getTipoPublicacao() { return tipoPublicacao; }
    public String getIdCategoria() { return idCategoria; }
    public String getNomeCategoria() { return nomeCategoria; }
    public String getIdSolicitante() { return idSolicitante; }
    public String getNomeSolicitante() { return nomeSolicitante; }
    public StatusPedido getStatus() { return status; }
    public LocalDateTime getDtInicio() { return dtInicio; }
    public LocalDateTime getDtFinal() { return dtFinal; }
    public Double getValor() { return valor; }
    public Boolean getNaoIncluirPagamento() { return naoIncluirPagamento; }
    public String getRecompensa() { return recompensa; }
    public Double getCaixinha() { return caixinha; }
    public Double getTotal() { return total; }
    public String getFotoUrl() { return fotoUrl; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getIdCandidaturaEscolhida() { return idCandidaturaEscolhida; }
    public String getIdAjudante() { return idAjudante; }
    public String getIdAjudado() { return idAjudado; }

    public String getIdPrestador() { return idPrestador; }
    public String getNomePrestador() { return nomePrestador; }
    public StatusPagamento getStatusPagamento() { return statusPagamento; }
    public LocalDateTime getDtPagamento() { return dtPagamento; }
    public LocalDateTime getDtConfirmacaoPagamento() { return dtConfirmacaoPagamento; }
    public String getEmailSolicitante() { return emailSolicitante; }
    public String getEmailPrestador() { return emailPrestador; }
}

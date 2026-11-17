package br.com.juntto.model;

import br.com.juntto.exception.AcessoNegadoException;
import br.com.juntto.exception.RegraNegocioException;

import java.time.LocalDateTime;

// Tabela tb_pedido (+ candidato escolhido e pagamento, que ficam na candidatura escolhida).
// idSolicitante = quem publicou | idPrestador = candidato escolhido
public class Pedido {

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
    private String fotoUrl;
    private Double latitude;
    private Double longitude;
    private LocalDateTime dtPagamento;
    private String idCandidaturaEscolhida;
    private String idPrestador;
    private String nomePrestador;
    private StatusPagamento statusPagamento;
    private LocalDateTime dtConfirmacaoPagamento;
    private String emailSolicitante;
    private String emailPrestador;

    public Pedido() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getTipoPublicacao() { return tipoPublicacao; }
    public void setTipoPublicacao(String tipoPublicacao) { this.tipoPublicacao = tipoPublicacao; }

    public String getIdCategoria() { return idCategoria; }
    public void setIdCategoria(String idCategoria) { this.idCategoria = idCategoria; }

    public String getNomeCategoria() { return nomeCategoria; }
    public void setNomeCategoria(String nomeCategoria) { this.nomeCategoria = nomeCategoria; }

    public String getIdSolicitante() { return idSolicitante; }
    public void setIdSolicitante(String idSolicitante) { this.idSolicitante = idSolicitante; }

    public String getNomeSolicitante() { return nomeSolicitante; }
    public void setNomeSolicitante(String nomeSolicitante) { this.nomeSolicitante = nomeSolicitante; }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public LocalDateTime getDtInicio() { return dtInicio; }
    public void setDtInicio(LocalDateTime dtInicio) { this.dtInicio = dtInicio; }

    public LocalDateTime getDtFinal() { return dtFinal; }
    public void setDtFinal(LocalDateTime dtFinal) { this.dtFinal = dtFinal; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public Boolean getNaoIncluirPagamento() { return naoIncluirPagamento; }
    public void setNaoIncluirPagamento(Boolean naoIncluirPagamento) { this.naoIncluirPagamento = naoIncluirPagamento; }

    public String getRecompensa() { return recompensa; }
    public void setRecompensa(String recompensa) { this.recompensa = recompensa; }

    public Double getCaixinha() { return caixinha; }
    public void setCaixinha(Double caixinha) { this.caixinha = caixinha; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getDtPagamento() { return dtPagamento; }
    public void setDtPagamento(LocalDateTime dtPagamento) { this.dtPagamento = dtPagamento; }

    public String getIdCandidaturaEscolhida() { return idCandidaturaEscolhida; }
    public void setIdCandidaturaEscolhida(String idCandidaturaEscolhida) { this.idCandidaturaEscolhida = idCandidaturaEscolhida; }

    public String getIdPrestador() { return idPrestador; }
    public void setIdPrestador(String idPrestador) { this.idPrestador = idPrestador; }

    public String getNomePrestador() { return nomePrestador; }
    public void setNomePrestador(String nomePrestador) { this.nomePrestador = nomePrestador; }

    public StatusPagamento getStatusPagamento() { return statusPagamento; }
    public void setStatusPagamento(StatusPagamento statusPagamento) { this.statusPagamento = statusPagamento; }

    public LocalDateTime getDtConfirmacaoPagamento() { return dtConfirmacaoPagamento; }
    public void setDtConfirmacaoPagamento(LocalDateTime dtConfirmacaoPagamento) { this.dtConfirmacaoPagamento = dtConfirmacaoPagamento; }

    public String getEmailSolicitante() { return emailSolicitante; }
    public void setEmailSolicitante(String emailSolicitante) { this.emailSolicitante = emailSolicitante; }

    public String getEmailPrestador() { return emailPrestador; }
    public void setEmailPrestador(String emailPrestador) { this.emailPrestador = emailPrestador; }

    // Prazo (em horas) para o prestador confirmar o recebimento do pagamento
    public static final int HORAS_PARA_CONFIRMAR_PAGAMENTO = 48;

    // Total = valor combinado + caixinha (só existe depois que o pagamento é registrado)
    public boolean semPagamento() {
        return Boolean.TRUE.equals(naoIncluirPagamento);
    }

    public Double getTotal() {
        if (statusPagamento != StatusPagamento.REGISTRADO && statusPagamento != StatusPagamento.CONFIRMADO) {
            return null;
        }
        return (valor == null ? 0 : valor) + (caixinha == null ? 0 : caixinha);
    }

    // ---------- papéis: em AJUDA quem publica recebe a ajuda; em OFERTA quem publica é o ajudante ----------

    public boolean ehOferta() {
        return TipoPublicacao.OFERTA.name().equals(tipoPublicacao);
    }

    // Quem faz o serviço
    public String getIdAjudante() {
        return ehOferta() ? idSolicitante : idPrestador;
    }

    // Quem recebe a ajuda
    public String getIdAjudado() {
        return ehOferta() ? idPrestador : idSolicitante;
    }

    public String nomeDoAjudante() {
        return ehOferta() ? nomeSolicitante : nomePrestador;
    }

    public String nomeDoAjudado() {
        return ehOferta() ? nomePrestador : nomeSolicitante;
    }

    public void validarAjudante(String usuarioId) {
        if (getIdAjudante() == null || !getIdAjudante().equals(usuarioId)) {
            throw new AcessoNegadoException("Somente quem está ajudando neste serviço pode fazer esta ação.");
        }
    }

    public void validarAjudado(String usuarioId) {
        if (getIdAjudado() == null || !getIdAjudado().equals(usuarioId)) {
            throw new AcessoNegadoException("Somente quem está recebendo a ajuda pode fazer esta ação.");
        }
    }

    // Dono da publicação (em AJUDA é quem pediu; em OFERTA é quem ofereceu)
    public boolean ehSolicitante(String usuarioId) {
        return idSolicitante != null && idSolicitante.equals(usuarioId);
    }

    public boolean ehPrestador(String usuarioId) {
        return idPrestador != null && idPrestador.equals(usuarioId);
    }

    public void validarSolicitante(String usuarioId) {
        if (!ehSolicitante(usuarioId)) {
            throw new AcessoNegadoException("Somente quem publicou pode fazer esta ação.");
        }
    }

    public void validarPrestador(String usuarioId) {
        if (!ehPrestador(usuarioId)) {
            throw new AcessoNegadoException("Somente o prestador escolhido para o pedido pode fazer esta ação.");
        }
    }

    public void validarStatus(StatusPedido esperado, String acao) {
        if (status != esperado) {
            throw new RegraNegocioException("Não é possível " + acao + ": o pedido está " + status + ".");
        }
    }

    // Regra: editar e excluir só enquanto o pedido está Aberto
    public void validarEditavel() {
        validarStatus(StatusPedido.ABERTO, "alterar o pedido");
    }

    public boolean prazoDePagamentoVencido(LocalDateTime agora) {
        return statusPagamento == StatusPagamento.REGISTRADO && dtPagamento != null
                && dtPagamento.plusHours(HORAS_PARA_CONFIRMAR_PAGAMENTO).isBefore(agora);
    }

}

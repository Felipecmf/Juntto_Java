package br.com.juntto.model;

import java.time.LocalDateTime;

// Tabela tb_notificacao: toda notificação está ligada a uma candidatura e a um pedido.
public class Notificacao {

    private String id;
    private String idUsuario;
    private String idCandidatura;
    private String idPedido;
    private TipoNotificacao tipo;
    private String mensagem;
    private boolean lida;
    private LocalDateTime dtCriacao;

    public Notificacao() {
    }

    public Notificacao(String idUsuario, TipoNotificacao tipo, String mensagem, String idPedido, String idCandidatura) {
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.idPedido = idPedido;
        this.idCandidatura = idCandidatura;
        this.lida = false;
        this.dtCriacao = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getIdCandidatura() { return idCandidatura; }
    public void setIdCandidatura(String idCandidatura) { this.idCandidatura = idCandidatura; }

    public String getIdPedido() { return idPedido; }
    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }

    public TipoNotificacao getTipo() { return tipo; }
    public void setTipo(TipoNotificacao tipo) { this.tipo = tipo; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }

    public LocalDateTime getDtCriacao() { return dtCriacao; }
    public void setDtCriacao(LocalDateTime dtCriacao) { this.dtCriacao = dtCriacao; }

}

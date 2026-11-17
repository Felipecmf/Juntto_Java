package br.com.juntto.dto;

import br.com.juntto.model.TipoNotificacao;

import java.time.LocalDateTime;

// [GS] Saída de notificação
public class NotificacaoResponse {

    private String id;
    private String idUsuario;
    private String idCandidatura;
    private String idPedido;
    private TipoNotificacao tipo;
    private String mensagem;
    private boolean lida;
    private LocalDateTime dtCriacao;

    public String getId() { return id; }
    public String getIdUsuario() { return idUsuario; }
    public String getIdCandidatura() { return idCandidatura; }
    public String getIdPedido() { return idPedido; }
    public TipoNotificacao getTipo() { return tipo; }
    public String getMensagem() { return mensagem; }
    public boolean getLida() { return lida; }
    public LocalDateTime getDtCriacao() { return dtCriacao; }
}

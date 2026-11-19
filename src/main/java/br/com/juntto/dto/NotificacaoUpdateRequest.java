package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// [GS] Entrada do PUT /notificacoes/{id}: marca como lida ou não lida (só o destinatário)
public class NotificacaoUpdateRequest {

    // quem está fazendo a ação: precisa ser o destinatário da notificação
    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotNull(message = "O campo é obrigatório.")
    @JsonProperty
    private Boolean lida;

    public NotificacaoUpdateRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public Boolean getLida() { return lida; }
}

package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

// [GS] Entrada do PUT /notificacoes/{id}: marca como lida ou não lida
public class NotificacaoUpdateRequest {

    @NotNull(message = "O campo é obrigatório.")
    @JsonProperty
    private Boolean lida;

    public NotificacaoUpdateRequest() {
    }

    public Boolean getLida() { return lida; }
}

package br.com.juntto.dto;

import br.com.juntto.model.StatusPedido;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Entrada do PATCH /pedidos/{id}/status
public class StatusPedidoRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotNull(message = "O campo é obrigatório.")
    @JsonProperty
    private StatusPedido status;

    public StatusPedidoRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public StatusPedido getStatus() { return status; }
}

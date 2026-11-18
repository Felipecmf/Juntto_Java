package br.com.juntto.dto;

import br.com.juntto.model.StatusPedido;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// Entrada do PATCH /pedidos/{id}/status: CONCLUIDO ou CANCELADO.
// caixinha (opcional) só vale ao concluir uma AJUDA com pagamento: quem recebeu a ajuda informa a gorjeta.
public class StatusPedidoRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotNull(message = "Informe o status: CONCLUIDO ou CANCELADO.")
    @JsonProperty
    private StatusPedido status;

    @PositiveOrZero(message = "A caixinha não pode ser negativa.")
    @JsonProperty
    private Double caixinha;

    public StatusPedidoRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public StatusPedido getStatus() { return status; }
    public Double getCaixinha() { return caixinha; }
}

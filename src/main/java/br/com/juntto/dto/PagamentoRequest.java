package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

// Entrada do POST /pedidos/{id}/pagamento: caixinha opcional
public class PagamentoRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @PositiveOrZero(message = "A caixinha não pode ser negativa.")
    @JsonProperty
    private Double caixinha;

    public PagamentoRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public Double getCaixinha() { return caixinha; }
}

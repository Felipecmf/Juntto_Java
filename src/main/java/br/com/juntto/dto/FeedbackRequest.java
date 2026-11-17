package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// [GS] Entrada do POST /pedidos/{id}/feedbacks e do PUT /feedbacks/{id}: estrelas de 1 a 5 e texto opcional
public class FeedbackRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotNull(message = "O campo é obrigatório.")
    @Min(value = 1, message = "As estrelas vão de 1 a 5.")
    @Max(value = 5, message = "As estrelas vão de 1 a 5.")
    @JsonProperty
    private Integer estrelas;

    @Size(max = 500, message = "Deve ter no máximo 500 caracteres.")
    @JsonProperty
    private String texto;

    public FeedbackRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public Integer getEstrelas() { return estrelas; }
    public String getTexto() { return texto; }
}

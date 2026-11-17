package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

// [GS] Entrada do POST /pedidos/{id}/candidaturas
public class CandidaturaRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    public CandidaturaRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
}

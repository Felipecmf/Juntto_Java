package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

// Identifica quem está fazendo a ação
public class UsuarioIdRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    public UsuarioIdRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
}

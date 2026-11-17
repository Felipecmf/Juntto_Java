package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [GS] Entrada do POST/PUT /categorias
public class CategoriaRequest {

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 2, max = 100, message = "Deve ter entre 2 e 100 caracteres.")
    @JsonProperty
    private String nome;

    public CategoriaRequest() {
    }

    public String getNome() { return nome; }
}

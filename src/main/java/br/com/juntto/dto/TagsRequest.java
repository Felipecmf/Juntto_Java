package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;

// Entrada do PUT /usuarios/{id}/tags: o conjunto inteiro de tags
public class TagsRequest {

    // quem está fazendo a ação: precisa ser o próprio dono da conta
    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotNull(message = "Envie a lista de categorias (pode ser vazia).")
    @JsonProperty
    private ArrayList<String> idsCategorias;

    public TagsRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public ArrayList<String> getIdsCategorias() { return idsCategorias; }
}

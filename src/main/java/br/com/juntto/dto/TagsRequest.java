package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;

// Entrada do PUT /usuarios/{id}/tags: o conjunto inteiro de tags
public class TagsRequest {

    @NotNull(message = "Envie a lista de categorias (pode ser vazia).")
    @JsonProperty
    private ArrayList<String> idsCategorias;

    public TagsRequest() {
    }

    public ArrayList<String> getIdsCategorias() { return idsCategorias; }
}

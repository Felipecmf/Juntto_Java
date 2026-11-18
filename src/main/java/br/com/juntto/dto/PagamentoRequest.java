package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

// Entrada do PATCH /pedidos/{id}/pagamento/confirmacao: quem recebeu confirma.
// idCandidatura só é necessário em OFERTA com mais de um interessado aceito.
public class PagamentoRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @JsonProperty
    private String idCandidatura;

    public PagamentoRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public String getIdCandidatura() { return idCandidatura; }
}

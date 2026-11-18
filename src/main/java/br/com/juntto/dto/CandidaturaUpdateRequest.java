package br.com.juntto.dto;

import br.com.juntto.model.StatusCandidatura;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// [GS] Entrada do PUT /candidaturas/{id}: quem publicou aceita (ACEITA) ou recusa (RECUSADA) um candidato
public class CandidaturaUpdateRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotNull(message = "Informe o status: ACEITA ou RECUSADA.")
    @JsonProperty
    private StatusCandidatura status;

    public CandidaturaUpdateRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public StatusCandidatura getStatus() { return status; }
}

package br.com.juntto.dto;

import br.com.juntto.model.StatusCandidatura;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// [GS] Entrada do PUT /candidaturas/{id}: o solicitante escolhe (ESCOLHIDO) ou recusa (RECUSADO) um candidato
public class CandidaturaUpdateRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotNull(message = "Informe o status: ESCOLHIDO ou RECUSADO.")
    @JsonProperty
    private StatusCandidatura status;

    public CandidaturaUpdateRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public StatusCandidatura getStatus() { return status; }
}

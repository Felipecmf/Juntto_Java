package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [GS] Entrada do POST /notificacoes: lembrete de um participante para o outro, ligado a uma candidatura
public class NotificacaoRequest {

    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotBlank(message = "O campo é obrigatório.")
    @JsonProperty
    private String idCandidatura;

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 3, max = 450, message = "Deve ter entre 3 e 450 caracteres.")
    @JsonProperty
    private String mensagem;

    public NotificacaoRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public String getIdCandidatura() { return idCandidatura; }
    public String getMensagem() { return mensagem; }
}

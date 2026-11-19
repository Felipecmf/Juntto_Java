package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [GS] Entrada do PUT /usuarios/{id} (e-mail e senha não mudam por aqui; só o dono da conta altera)
public class UsuarioUpdateRequest {

    // quem está fazendo a ação: precisa ser o próprio dono da conta
    @NotBlank(message = "Informe o usuarioId (usuário que está fazendo a ação).")
    @JsonProperty
    private String usuarioId;

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 2, max = 20, message = "Deve ter entre 2 e 20 caracteres.")
    @JsonProperty
    private String nome;

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 2, max = 20, message = "Deve ter entre 2 e 20 caracteres.")
    @JsonProperty
    private String sobrenome;

    @Size(max = 200, message = "Deve ter no máximo 200 caracteres.")
    @JsonProperty
    private String fotoUrl;

    @DecimalMin(value = "-90", message = "Latitude inválida.")
    @DecimalMax(value = "90", message = "Latitude inválida.")
    @JsonProperty
    private Double latitude;

    @DecimalMin(value = "-180", message = "Longitude inválida.")
    @DecimalMax(value = "180", message = "Longitude inválida.")
    @JsonProperty
    private Double longitude;

    public UsuarioUpdateRequest() {
    }

    public String getUsuarioId() { return usuarioId; }
    public String getNome() { return nome; }
    public String getSobrenome() { return sobrenome; }
    public String getFotoUrl() { return fotoUrl; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}

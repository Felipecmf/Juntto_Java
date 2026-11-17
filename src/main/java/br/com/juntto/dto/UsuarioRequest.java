package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [GS] Entrada do POST /usuarios (cadastro), com Bean Validation
public class UsuarioRequest {

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 2, max = 20, message = "Deve ter entre 2 e 20 caracteres.")
    @JsonProperty
    private String nome;

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 2, max = 20, message = "Deve ter entre 2 e 20 caracteres.")
    @JsonProperty
    private String sobrenome;

    @NotBlank(message = "O campo é obrigatório.")
    @Email(message = "E-mail inválido.")
    @Size(max = 100, message = "Deve ter no máximo 100 caracteres.")
    @JsonProperty
    private String email;

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 6, max = 50, message = "Deve ter entre 6 e 50 caracteres.")
    @JsonProperty
    private String senha;

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

    public UsuarioRequest() {
    }

    public String getNome() { return nome; }
    public String getSobrenome() { return sobrenome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getFotoUrl() { return fotoUrl; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}

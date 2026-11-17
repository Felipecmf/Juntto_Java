package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Entrada do POST /login
public class LoginRequest {

    @NotBlank(message = "O campo é obrigatório.")
    @Email(message = "E-mail inválido.")
    @JsonProperty
    private String email;

    @NotBlank(message = "O campo é obrigatório.")
    @JsonProperty
    private String senha;

    public LoginRequest() {
    }

    public String getEmail() { return email; }
    public String getSenha() { return senha; }
}

package br.com.juntto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// [GS] Entrada do POST/PUT /pedidos. valor é obrigatório, a não ser que naoIncluirPagamento seja true
public class PedidoRequest {

    @NotBlank(message = "Informe o idSolicitante (usuário que está publicando).")
    @JsonProperty
    private String idSolicitante;

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 3, max = 50, message = "Deve ter entre 3 e 50 caracteres.")
    @JsonProperty
    private String titulo;

    @NotBlank(message = "O campo é obrigatório.")
    @Size(min = 5, max = 500, message = "Deve ter entre 5 e 500 caracteres.")
    @JsonProperty
    private String descricao;

    @Size(max = 50, message = "Deve ter no máximo 50 caracteres.")
    @JsonProperty
    private String tipoPublicacao;

    @NotBlank(message = "O campo é obrigatório.")
    @JsonProperty
    private String idCategoria;

    @Positive(message = "O valor precisa ser maior que zero.")
    @JsonProperty
    private Double valor;

    @JsonProperty
    private Boolean naoIncluirPagamento;

    @Size(max = 300, message = "Deve ter no máximo 300 caracteres.")
    @JsonProperty
    private String recompensa;

    @NotNull(message = "O campo é obrigatório.")
    @Future(message = "A data e o horário não podem estar no passado.")
    @JsonProperty
    private LocalDateTime dtInicio;

    @Future(message = "A data final não pode estar no passado.")
    @JsonProperty
    private LocalDateTime dtFinal;

    @Size(max = 200, message = "Deve ter no máximo 200 caracteres.")
    @JsonProperty
    private String fotoUrl;

    @NotNull(message = "O campo é obrigatório.")
    @DecimalMin(value = "-90", message = "Latitude inválida.")
    @DecimalMax(value = "90", message = "Latitude inválida.")
    @JsonProperty
    private Double latitude;

    @NotNull(message = "O campo é obrigatório.")
    @DecimalMin(value = "-180", message = "Longitude inválida.")
    @DecimalMax(value = "180", message = "Longitude inválida.")
    @JsonProperty
    private Double longitude;

    public PedidoRequest() {
    }

    public String getIdSolicitante() { return idSolicitante; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getTipoPublicacao() { return tipoPublicacao; }
    public String getIdCategoria() { return idCategoria; }
    public Double getValor() { return valor; }
    public Boolean getNaoIncluirPagamento() { return naoIncluirPagamento; }
    public String getRecompensa() { return recompensa; }
    public LocalDateTime getDtInicio() { return dtInicio; }
    public LocalDateTime getDtFinal() { return dtFinal; }
    public String getFotoUrl() { return fotoUrl; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}

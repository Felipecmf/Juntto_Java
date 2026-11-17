package br.com.juntto.dto;

// [GS] Saída de usuário (a senha nunca é devolvida; no perfil público e-mail e localização vêm vazios)
public class UsuarioResponse {

    private String id;
    private String nome;
    private String sobrenome;
    private String email;
    private String fotoUrl;
    private Double latitude;
    private Double longitude;
    private Double mediaEstrelas;
    private Integer totalAvaliacoes;

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getSobrenome() { return sobrenome; }
    public String getEmail() { return email; }
    public String getFotoUrl() { return fotoUrl; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getMediaEstrelas() { return mediaEstrelas; }
    public Integer getTotalAvaliacoes() { return totalAvaliacoes; }
}

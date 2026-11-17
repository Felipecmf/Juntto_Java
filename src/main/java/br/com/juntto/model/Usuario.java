package br.com.juntto.model;

// Tabela tb_usuario. A mesma conta pode pedir e oferecer ajuda.
public class Usuario {

    private String id;
    private String nome;
    private String sobrenome;
    private String email;
    private String senha;
    private String fotoUrl;
    private Double latitude;
    private Double longitude;
    private Double mediaEstrelas;
    private Integer totalAvaliacoes;

    public Usuario() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSobrenome() { return sobrenome; }
    public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getMediaEstrelas() { return mediaEstrelas; }
    public void setMediaEstrelas(Double mediaEstrelas) { this.mediaEstrelas = mediaEstrelas; }

    public Integer getTotalAvaliacoes() { return totalAvaliacoes; }
    public void setTotalAvaliacoes(Integer totalAvaliacoes) { this.totalAvaliacoes = totalAvaliacoes; }

    // Conta excluída: os dados pessoais foram apagados e o e-mail virou "removido-<id>@juntto.com"
    public boolean removido() {
        return email != null && email.startsWith("removido-");
    }

    // Perfil público: nome, foto e avaliações (sem e-mail e sem localização)
    public Usuario perfilPublico() {
        Usuario p = new Usuario();
        p.setId(id);
        p.setNome(nome);
        p.setSobrenome(sobrenome);
        p.setFotoUrl(fotoUrl);
        p.setMediaEstrelas(mediaEstrelas);
        p.setTotalAvaliacoes(totalAvaliacoes);
        return p;
    }

}

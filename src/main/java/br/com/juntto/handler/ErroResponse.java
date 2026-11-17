package br.com.juntto.handler;

import java.time.LocalDateTime;
import java.util.ArrayList;

// [GS] Formato padronizado de todo erro devolvido pela API
public class ErroResponse {

    private LocalDateTime timestamp;
    private int status;
    private String erro;
    private String mensagem;
    private String caminho;
    private ArrayList<String> campos;

    public ErroResponse(int status, String erro, String mensagem, String caminho, ArrayList<String> campos) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
        this.caminho = caminho;
        this.campos = campos;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getErro() { return erro; }
    public String getMensagem() { return mensagem; }
    public String getCaminho() { return caminho; }
    public ArrayList<String> getCampos() { return campos; }
}

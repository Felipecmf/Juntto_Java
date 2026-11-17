package br.com.juntto.exception;

// [GS] Exceção própria do domínio: dado de entrada inválido (HTTP 400)
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}

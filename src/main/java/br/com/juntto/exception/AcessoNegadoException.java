package br.com.juntto.exception;

// [GS] Exceção própria do domínio: usuário sem permissão para a ação (HTTP 403)
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}

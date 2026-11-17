package br.com.juntto.exception;

// [GS] Exceção própria do domínio: registro que não existe (HTTP 404)
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}

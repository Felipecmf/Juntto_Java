package br.com.juntto.exception;

// [GS] Exceção própria do domínio: regra de negócio violada ou remoção de registro com vínculos (HTTP 409)
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}

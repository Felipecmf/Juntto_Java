package br.com.juntto.handler;

import br.com.juntto.exception.AcessoNegadoException;
import br.com.juntto.exception.DadosInvalidosException;
import br.com.juntto.exception.RecursoNaoEncontradoException;
import br.com.juntto.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;

// [GS] Handler global: transforma qualquer exceção em um JSON padronizado (sem stack trace e sem SQL).
// Substitui o try/catch + RespostaDeErro que cada controller fazia no UrbanLink.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 - Bean Validation reprovou algum campo do DTO (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        ArrayList<String> campos = new ArrayList<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.add(erro.getField() + ": " + erro.getDefaultMessage());
        }
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique os campos.", req, campos);
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResponse> dadosInvalidos(DadosInvalidosException e, HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST, e.getMessage(), req, null);
    }

    // 400 - JSON mal formatado, parâmetro faltando ou com tipo errado
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> requisicaoInvalida(Exception e, HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST, "Requisição inválida: verifique o JSON e os parâmetros enviados.", req, null);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AcessoNegadoException e, HttpServletRequest req) {
        return montar(HttpStatus.FORBIDDEN, e.getMessage(), req, null);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND, e.getMessage(), req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException e, HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND, "Rota não encontrada.", req, null);
    }

    // 405 - método HTTP não suportado na rota
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return montar(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " não suportado nesta rota.", req, null);
    }

    // 409 - regra de negócio violada
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e, HttpServletRequest req) {
        return montar(HttpStatus.CONFLICT, e.getMessage(), req, null);
    }

    // 500 - erro inesperado: o detalhe fica só no log do servidor
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e, HttpServletRequest req) {
        e.printStackTrace();
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição. Tente novamente.", req, null);
    }

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String mensagem, HttpServletRequest req,
                                                ArrayList<String> campos) {
        ErroResponse corpo = new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, req.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }
}

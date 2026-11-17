package br.com.juntto.dao;

import br.com.juntto.exception.DadosInvalidosException;
import br.com.juntto.exception.RegraNegocioException;

import java.sql.SQLException;

// Traduz os erros do Oracle em exceções do domínio (o detalhe do SQL nunca chega ao cliente)
class ErroSql {

    static RuntimeException traduzir(SQLException e) {
        int codigo = e.getErrorCode();
        if (codigo == 1) {
            return new RegraNegocioException("Já existe um registro com estes dados (ex.: e-mail repetido).");
        }
        if (codigo == 2290) {
            return new DadosInvalidosException("Algum valor informado não respeita as regras do cadastro.");
        }
        if (codigo == 2291) {
            return new DadosInvalidosException("Referência inválida: o registro relacionado não existe.");
        }
        if (codigo == 2292) {
            return new RegraNegocioException("O registro possui vínculos e não pode ser removido.");
        }
        if (codigo == 12899 || codigo == 1438) {
            return new DadosInvalidosException("Algum valor informado excede o limite permitido.");
        }
        return new RuntimeException(e);
    }
}

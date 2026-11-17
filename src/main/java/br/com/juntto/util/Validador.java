package br.com.juntto.util;

import br.com.juntto.exception.DadosInvalidosException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class Validador {

    public static String texto(String valor, String campo, int min, int max) {
        String t = valor == null ? "" : valor.trim();
        if (t.length() < min) {
            throw new DadosInvalidosException("O campo '" + campo + "' é obrigatório (mínimo de " + min + " caracteres).");
        }
        if (t.length() > max) {
            throw new DadosInvalidosException("O campo '" + campo + "' excede " + max + " caracteres.");
        }
        return t;
    }

    // Campo opcional: vazio vira null; preenchido respeita o tamanho máximo
    public static String textoOpcional(String valor, String campo, int max) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return texto(valor, campo, 1, max);
    }

    public static String email(String valor) {
        String t = texto(valor, "email", 5, 100).toLowerCase();
        int arroba = t.indexOf("@");
        boolean valido = arroba > 0
                && arroba == t.lastIndexOf("@")
                && t.indexOf(".", arroba) > arroba + 1
                && !t.endsWith(".")
                && !t.contains(" ");
        if (!valido) {
            throw new DadosInvalidosException("E-mail inválido.");
        }
        return t;
    }

    // Aceita "00000000" ou "00000-000" e devolve sempre "00000-000"
    public static String cep(String valor) {
        String digitos = valor == null ? "" : valor.replaceAll("[^0-9]", "");
        if (digitos.length() != 8) {
            throw new DadosInvalidosException("CEP inválido. Use o formato 00000-000.");
        }
        return digitos.substring(0, 5) + "-" + digitos.substring(5);
    }

    public static LocalDateTime dataFutura(LocalDateTime valor, String campo) {
        if (valor == null) {
            throw new DadosInvalidosException("O campo '" + campo + "' é obrigatório.");
        }
        if (!valor.isAfter(LocalDateTime.now())) {
            throw new DadosInvalidosException("O campo '" + campo + "' precisa ser uma data futura.");
        }
        return valor;
    }

    // Filtro opcional de data no formato AAAA-MM-DD
    public static LocalDate dataOpcional(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new DadosInvalidosException("O campo '" + campo + "' deve estar no formato AAAA-MM-DD.");
        }
    }

    public static double numero(Double valor, String campo, double min, double max) {
        if (valor == null) {
            throw new DadosInvalidosException("O campo '" + campo + "' é obrigatório.");
        }
        if (valor.isNaN() || valor.isInfinite() || valor < min || valor > max) {
            throw new DadosInvalidosException("O campo '" + campo + "' deve estar entre " + min + " e " + max + ".");
        }
        return valor;
    }

    public static int inteiro(Integer valor, String campo, int min, int max) {
        if (valor == null) {
            throw new DadosInvalidosException("O campo '" + campo + "' é obrigatório.");
        }
        if (valor < min || valor > max) {
            throw new DadosInvalidosException("O campo '" + campo + "' deve estar entre " + min + " e " + max + ".");
        }
        return valor;
    }

    public static <T> T obrigatorio(T valor, String campo) {
        if (valor == null) {
            throw new DadosInvalidosException("O campo '" + campo + "' é obrigatório.");
        }
        return valor;
    }
}

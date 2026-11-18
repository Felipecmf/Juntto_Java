package br.com.juntto.dao;

import br.com.juntto.model.Candidatura;
import br.com.juntto.model.StatusCandidatura;
import br.com.juntto.model.StatusPagamento;

import java.time.LocalDateTime;
import java.util.ArrayList;

public interface RepositorioCandidaturas {

    void adicionar(Candidatura candidatura);

    Candidatura buscarPorId(String id);

    ArrayList<Candidatura> obterTodas();

    ArrayList<Candidatura> obterPorPedido(String idPedido);

    ArrayList<Candidatura> obterPorUsuario(String usuarioId);

    ArrayList<Candidatura> obterPendentes(String idPedido);

    ArrayList<Candidatura> obterAceitas(String idPedido);

    ArrayList<Candidatura> obterPagamentosPendentesAte(LocalDateTime limite);

    void alterarStatusPagamentoDasPendentes(String idPedido, StatusPagamento status);

    boolean existe(String idPedido, String usuarioId);

    void alterarStatus(String id, StatusCandidatura status);

    void alterarStatusPagamento(String id, StatusPagamento status, LocalDateTime dtConfirmacao);
}

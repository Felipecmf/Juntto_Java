package br.com.juntto.dao;

import br.com.juntto.model.Feedback;

import java.util.ArrayList;

public interface RepositorioFeedbacks {

    void adicionar(Feedback feedback);

    Feedback buscarPorId(String id);

    Feedback buscarPorCandidatura(String idCandidatura);

    ArrayList<Feedback> obterTodos();

    ArrayList<Feedback> obterPorPedido(String idPedido);

    ArrayList<Feedback> obterRecebidos(String usuarioId);

    boolean atualizar(Feedback feedback);

    boolean remover(String id);
}

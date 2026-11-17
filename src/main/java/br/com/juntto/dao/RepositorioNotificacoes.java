package br.com.juntto.dao;

import br.com.juntto.model.Notificacao;

import java.util.ArrayList;

public interface RepositorioNotificacoes {

    void adicionar(Notificacao notificacao);

    Notificacao buscarPorId(String id);

    ArrayList<Notificacao> obterTodas();

    ArrayList<Notificacao> obterPorUsuario(String usuarioId, Boolean lida);

    boolean atualizarLida(String id, boolean lida);

    boolean remover(String id);
}

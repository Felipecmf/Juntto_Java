package br.com.juntto.dao;

import br.com.juntto.model.Categoria;

import java.util.ArrayList;

public interface RepositorioCategorias {

    void adicionar(Categoria categoria);

    boolean atualizar(Categoria categoria);

    Categoria buscarPorId(String id);

    ArrayList<Categoria> obterTodas();

    boolean emUso(String id);

    boolean remover(String id);
}

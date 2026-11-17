package br.com.juntto.dao;

import br.com.juntto.model.Categoria;
import br.com.juntto.model.Usuario;

import java.util.ArrayList;

public interface RepositorioUsuarios {

    void adicionar(Usuario usuario);

    boolean atualizar(Usuario usuario);

    Usuario buscarPorId(String id);

    Usuario buscarPorEmail(String email);

    ArrayList<Usuario> obterTodos();

    boolean possuiVinculos(String id);

    void remover(String id);

    void anonimizar(String id);

    ArrayList<Categoria> obterTags(String id);

    void salvarTags(String id, ArrayList<String> idsCategorias);
}

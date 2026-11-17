package br.com.juntto.service;

import br.com.juntto.dao.RepositorioCategorias;
import br.com.juntto.exception.RecursoNaoEncontradoException;
import br.com.juntto.exception.RegraNegocioException;
import br.com.juntto.model.Categoria;

import java.util.ArrayList;

public class CategoriaService {

    private RepositorioCategorias repositorio;

    public CategoriaService(RepositorioCategorias repositorio) {
        this.repositorio = repositorio;
    }

    public ArrayList<Categoria> listar() {
        return repositorio.obterTodas();
    }

    public Categoria buscar(String id) {
        Categoria c = repositorio.buscarPorId(id);
        if (c == null) {
            throw new RecursoNaoEncontradoException("Categoria não encontrada: " + id);
        }
        return c;
    }

    public Categoria criar(String nome) {
        Categoria c = new Categoria(null, nome.trim());
        repositorio.adicionar(c);
        System.out.println("[AUDITORIA] Categoria criada: " + c.getId());
        return c;
    }

    public Categoria atualizar(String id, String nome) {
        Categoria c = buscar(id);
        c.setNome(nome.trim());
        repositorio.atualizar(c);
        return c;
    }

    // Regra: categoria usada por pedidos ou tags não pode ser removida
    public void remover(String id) {
        buscar(id);
        if (repositorio.emUso(id)) {
            throw new RegraNegocioException("A categoria está em uso por pedidos ou tags e não pode ser removida.");
        }
        repositorio.remover(id);
        System.out.println("[AUDITORIA] Categoria removida: " + id);
    }
}

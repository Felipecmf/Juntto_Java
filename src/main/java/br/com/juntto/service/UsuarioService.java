package br.com.juntto.service;

import br.com.juntto.dao.RepositorioCandidaturas;
import br.com.juntto.dao.RepositorioCategorias;
import br.com.juntto.dao.RepositorioPedidos;
import br.com.juntto.dao.RepositorioUsuarios;
import br.com.juntto.exception.DadosInvalidosException;
import br.com.juntto.exception.RecursoNaoEncontradoException;
import br.com.juntto.exception.RegraNegocioException;
import br.com.juntto.model.Candidatura;
import br.com.juntto.model.Categoria;
import br.com.juntto.model.Pedido;
import br.com.juntto.model.StatusCandidatura;
import br.com.juntto.model.StatusPedido;
import br.com.juntto.model.Usuario;
import br.com.juntto.util.Senha;
import br.com.juntto.util.Validador;

import java.util.ArrayList;

public class UsuarioService {

    private RepositorioUsuarios repositorioUsuarios;
    private RepositorioPedidos repositorioPedidos;
    private RepositorioCategorias repositorioCategorias;
    private RepositorioCandidaturas repositorioCandidaturas;

    public UsuarioService(RepositorioUsuarios repositorioUsuarios, RepositorioPedidos repositorioPedidos,
                          RepositorioCategorias repositorioCategorias, RepositorioCandidaturas repositorioCandidaturas) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioPedidos = repositorioPedidos;
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioCandidaturas = repositorioCandidaturas;
    }

    // Regra: e-mail único (o banco também garante com tb_usuario_email_uc)
    public Usuario cadastrar(Usuario usuario) {
        usuario.setEmail(Validador.email(usuario.getEmail()));
        if (repositorioUsuarios.buscarPorEmail(usuario.getEmail()) != null) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }
        usuario.setNome(usuario.getNome().trim());
        usuario.setSobrenome(usuario.getSobrenome().trim());
        usuario.setFotoUrl(Validador.textoOpcional(usuario.getFotoUrl(), "fotoUrl", 200));
        usuario.setSenha(Senha.gerarHash(usuario.getSenha()));
        repositorioUsuarios.adicionar(usuario);
        System.out.println("[AUDITORIA] Usuário cadastrado: " + usuario.getId());
        return repositorioUsuarios.buscarPorId(usuario.getId());
    }

    public Usuario login(String email, String senha) {
        Usuario u = repositorioUsuarios.buscarPorEmail(email.trim());
        if (u == null || u.removido() || !Senha.gerarHash(senha).equals(u.getSenha())) {
            throw new DadosInvalidosException("E-mail ou senha incorretos.");
        }
        System.out.println("[AUDITORIA] Login: " + u.getId());
        return u;
    }

    public ArrayList<Usuario> listar() {
        ArrayList<Usuario> publicos = new ArrayList<>();
        for (Usuario u : repositorioUsuarios.obterTodos()) {
            publicos.add(u.perfilPublico());
        }
        return publicos;
    }

    // O próprio usuário (usuarioId = id) vê todos os dados; os outros veem o perfil público
    public Usuario buscar(String id, String usuarioId) {
        Usuario u = buscarExistente(id);
        return id.equals(usuarioId) ? u : u.perfilPublico();
    }

    public Usuario atualizar(String id, Usuario novo) {
        Usuario u = buscarExistente(id);
        u.setNome(novo.getNome().trim());
        u.setSobrenome(novo.getSobrenome().trim());
        u.setFotoUrl(Validador.textoOpcional(novo.getFotoUrl(), "fotoUrl", 200));
        u.setLatitude(novo.getLatitude());
        u.setLongitude(novo.getLongitude());
        repositorioUsuarios.atualizar(u);
        System.out.println("[AUDITORIA] Usuário atualizado: " + id);
        return repositorioUsuarios.buscarPorId(id);
    }

    // Regra: não exclui com serviço em andamento. Cancela os pedidos Abertos e retira as candidaturas pendentes.
    // Com histórico (pedidos ou candidaturas), a conta é anonimizada; sem histórico, é apagada.
    public void remover(String id) {
        buscarExistente(id);
        if (repositorioPedidos.contarAtivosDoUsuario(id) > 0) {
            throw new RegraNegocioException("Existe um serviço em andamento. Conclua ou cancele antes de excluir a conta.");
        }
        for (Pedido p : repositorioPedidos.obterPorSolicitante(id)) {
            if (p.getStatus() == StatusPedido.ABERTO) {
                repositorioPedidos.alterarStatus(p.getId(), StatusPedido.CANCELADO);
                for (Candidatura pendente : repositorioCandidaturas.obterPendentes(p.getId())) {
                    repositorioCandidaturas.alterarStatus(pendente.getId(), StatusCandidatura.RECUSADA);
                }
            }
        }
        for (Candidatura c : repositorioCandidaturas.obterPorUsuario(id)) {
            if (c.getStatus() == StatusCandidatura.PENDENTE) {
                repositorioCandidaturas.alterarStatus(c.getId(), StatusCandidatura.CANCELADA);
            }
        }
        if (repositorioUsuarios.possuiVinculos(id)) {
            repositorioUsuarios.anonimizar(id);
        } else {
            repositorioUsuarios.remover(id);
        }
        System.out.println("[AUDITORIA] Conta excluída: " + id);
    }

    public ArrayList<Categoria> obterTags(String id) {
        buscarExistente(id);
        return repositorioUsuarios.obterTags(id);
    }

    public ArrayList<Categoria> salvarTags(String id, ArrayList<String> idsCategorias) {
        buscarExistente(id);
        ArrayList<String> semRepetidos = new ArrayList<>();
        for (String idCategoria : idsCategorias) {
            if (repositorioCategorias.buscarPorId(idCategoria) == null) {
                throw new DadosInvalidosException("Categoria não encontrada: " + idCategoria);
            }
            if (!semRepetidos.contains(idCategoria)) {
                semRepetidos.add(idCategoria);
            }
        }
        repositorioUsuarios.salvarTags(id, semRepetidos);
        return repositorioUsuarios.obterTags(id);
    }

    private Usuario buscarExistente(String id) {
        Usuario u = repositorioUsuarios.buscarPorId(id);
        if (u == null || u.removido()) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + id);
        }
        return u;
    }
}

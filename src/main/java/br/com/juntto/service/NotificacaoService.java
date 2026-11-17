package br.com.juntto.service;

import br.com.juntto.dao.RepositorioCandidaturas;
import br.com.juntto.dao.RepositorioNotificacoes;
import br.com.juntto.dao.RepositorioUsuarios;
import br.com.juntto.dto.NotificacaoRequest;
import br.com.juntto.exception.AcessoNegadoException;
import br.com.juntto.exception.RecursoNaoEncontradoException;
import br.com.juntto.model.Candidatura;
import br.com.juntto.model.Notificacao;
import br.com.juntto.model.TipoNotificacao;
import br.com.juntto.model.Usuario;

import java.util.ArrayList;

// As notificações automáticas são criadas pelo Notificador. Aqui ficam o CRUD e o lembrete manual.
public class NotificacaoService {

    private RepositorioNotificacoes repositorio;
    private RepositorioCandidaturas repositorioCandidaturas;
    private RepositorioUsuarios repositorioUsuarios;
    private PedidoService pedidoService;

    public NotificacaoService(RepositorioNotificacoes repositorio, RepositorioCandidaturas repositorioCandidaturas,
                              RepositorioUsuarios repositorioUsuarios, PedidoService pedidoService) {
        this.repositorio = repositorio;
        this.repositorioCandidaturas = repositorioCandidaturas;
        this.repositorioUsuarios = repositorioUsuarios;
        this.pedidoService = pedidoService;
    }

    // POST /notificacoes: lembrete de um participante da candidatura (solicitante ou candidato) para o outro
    public Notificacao criarLembrete(NotificacaoRequest pedido) {
        Candidatura c = repositorioCandidaturas.buscarPorId(pedido.getIdCandidatura());
        if (c == null) {
            throw new RecursoNaoEncontradoException("Candidatura não encontrada: " + pedido.getIdCandidatura());
        }
        String remetente = pedido.getUsuarioId();
        String destinatario;
        if (c.ehDoSolicitante(remetente)) {
            destinatario = c.getIdPrestador();
        } else if (c.ehDoPrestador(remetente)) {
            destinatario = c.getIdSolicitante();
        } else {
            throw new AcessoNegadoException("Somente quem pediu e o candidato podem enviar lembretes nesta candidatura.");
        }
        Usuario autor = repositorioUsuarios.buscarPorId(remetente);
        Notificacao n = new Notificacao(destinatario, TipoNotificacao.LEMBRETE,
                "Lembrete de " + autor.getNome() + ": " + pedido.getMensagem().trim(), c.getIdPedido(), c.getId());
        repositorio.adicionar(n);
        System.out.println("[AUDITORIA] Lembrete " + n.getId() + " de " + remetente + " para " + destinatario);
        return n;
    }

    // usuarioId = notificações do usuário (lida=false traz só as não lidas) | sem usuarioId = todas
    public ArrayList<Notificacao> listar(String usuarioId, Boolean lida) {
        pedidoService.verificarPrazos();   // gera os avisos de prazos vencidos antes de listar
        if (usuarioId == null || usuarioId.isBlank()) {
            return repositorio.obterTodas();
        }
        return repositorio.obterPorUsuario(usuarioId, lida);
    }

    public Notificacao buscar(String id) {
        Notificacao n = repositorio.buscarPorId(id);
        if (n == null) {
            throw new RecursoNaoEncontradoException("Notificação não encontrada: " + id);
        }
        return n;
    }

    public Notificacao atualizar(String id, boolean lida) {
        buscar(id);
        repositorio.atualizarLida(id, lida);
        return repositorio.buscarPorId(id);
    }

    public void remover(String id) {
        buscar(id);
        repositorio.remover(id);
    }
}

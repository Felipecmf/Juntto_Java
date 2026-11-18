package br.com.juntto.service;

import br.com.juntto.dao.RepositorioCandidaturas;
import br.com.juntto.dao.RepositorioPedidos;
import br.com.juntto.dao.RepositorioUsuarios;
import br.com.juntto.exception.DadosInvalidosException;
import br.com.juntto.exception.RecursoNaoEncontradoException;
import br.com.juntto.exception.RegraNegocioException;
import br.com.juntto.model.Candidatura;
import br.com.juntto.model.Pedido;
import br.com.juntto.model.StatusCandidatura;
import br.com.juntto.model.StatusPedido;
import br.com.juntto.model.TipoNotificacao;
import br.com.juntto.model.Usuario;

import java.time.LocalDateTime;
import java.util.ArrayList;

// Candidatura: alguém se interessa pela publicação e quem publicou aceita ou recusa.
// AJUDA: só um candidato é aceito (o pedido vai para EM_ANDAMENTO); os outros ficam na fila (PENDENTE)
//        e, se o aceito desistir, o pedido volta a ficar ABERTO para aceitar outro.
// OFERTA: vários interessados podem ser aceitos; a oferta continua ABERTA até quem ofereceu concluir.
public class CandidaturaService {

    private RepositorioCandidaturas repositorioCandidaturas;
    private RepositorioPedidos repositorioPedidos;
    private RepositorioUsuarios repositorioUsuarios;
    private Notificador notificador;

    public CandidaturaService(RepositorioCandidaturas repositorioCandidaturas, RepositorioPedidos repositorioPedidos,
                              RepositorioUsuarios repositorioUsuarios, Notificador notificador) {
        this.repositorioCandidaturas = repositorioCandidaturas;
        this.repositorioPedidos = repositorioPedidos;
        this.repositorioUsuarios = repositorioUsuarios;
        this.notificador = notificador;
    }

    // POST /pedidos/{id}/candidaturas
    // Regras: só pedidos Abertos, ninguém se candidata ao próprio pedido, uma candidatura por pessoa
    // e quem desistiu não pode se candidatar de novo ao mesmo pedido
    public Candidatura candidatar(String idPedido, String usuarioId) {
        Usuario prestador = repositorioUsuarios.buscarPorId(usuarioId);
        if (prestador == null || prestador.removido()) {
            throw new DadosInvalidosException("Usuário não encontrado: " + usuarioId);
        }
        Pedido p = buscarPedido(idPedido);
        if (p.ehSolicitante(usuarioId)) {
            throw new RegraNegocioException("Você não pode se candidatar ao próprio pedido.");
        }
        if (p.getStatus() != StatusPedido.ABERTO) {
            throw new RegraNegocioException("Este pedido não está recebendo candidaturas (status: " + p.getStatus() + ").");
        }
        if (p.getDtInicio() != null && !p.getDtInicio().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A data deste pedido já passou.");
        }
        if (repositorioCandidaturas.existe(idPedido, usuarioId)) {
            throw new RegraNegocioException("Você já se candidatou a este pedido (quem desistiu não pode se candidatar de novo).");
        }

        Candidatura c = new Candidatura();
        c.setIdPedido(idPedido);
        c.setIdPrestador(usuarioId);
        c.setStatus(StatusCandidatura.PENDENTE);
        c.setStatusPagamento(PedidoService.statusPagamentoInicial(p));
        c.setDtOferta(LocalDateTime.now());
        repositorioCandidaturas.adicionar(c);
        notificador.enviar(p.getIdSolicitante(), TipoNotificacao.NOVA_CANDIDATURA,
                prestador.getNome() + " se candidatou ao seu pedido \"" + p.getTitulo() + "\".", idPedido, c.getId());
        System.out.println("[AUDITORIA] Candidatura: pedido=" + idPedido + " prestador=" + usuarioId);
        return repositorioCandidaturas.buscarPorId(c.getId());
    }

    // GET /candidaturas?usuarioId= (minhas candidaturas) ou todas, sem filtro
    public ArrayList<Candidatura> listar(String usuarioId) {
        ArrayList<Candidatura> lista = usuarioId != null && !usuarioId.isBlank()
                ? repositorioCandidaturas.obterPorUsuario(usuarioId) : repositorioCandidaturas.obterTodas();
        for (Candidatura c : lista) {
            c.setEmailPrestador(null);
        }
        return lista;
    }

    // GET /pedidos/{id}/candidaturas?usuarioId=: só o dono do pedido vê todos os candidatos
    // (o e-mail de contato aparece só nas candidaturas aceitas)
    public ArrayList<Candidatura> listarPorPedido(String idPedido, String usuarioId) {
        Pedido p = buscarPedido(idPedido);
        p.validarSolicitante(usuarioId);
        ArrayList<Candidatura> lista = repositorioCandidaturas.obterPorPedido(idPedido);
        for (Candidatura c : lista) {
            if (!c.aceita()) {
                c.setEmailPrestador(null);
            }
        }
        return lista;
    }

    // GET /candidaturas/{id}?usuarioId=: o candidato ou o dono do pedido
    public Candidatura buscar(String id, String usuarioId) {
        Candidatura c = buscarExistente(id);
        c.validarVisualizacao(usuarioId);
        if (!c.aceita() || !c.ehDoSolicitante(usuarioId)) {
            c.setEmailPrestador(null);
        }
        return c;
    }

    // PUT /candidaturas/{id}: quem publicou ACEITA ou RECUSA um candidato pendente
    public Candidatura atualizar(String id, String usuarioId, StatusCandidatura novo) {
        Candidatura c = buscarExistente(id);
        Pedido p = buscarPedido(c.getIdPedido());
        p.validarSolicitante(usuarioId);
        if (c.getStatus() != StatusCandidatura.PENDENTE) {
            throw new RegraNegocioException("Só é possível alterar uma candidatura PENDENTE (esta está " + c.getStatus() + ").");
        }
        if (novo == StatusCandidatura.ACEITA) {
            aceitar(c, p);
        } else if (novo == StatusCandidatura.RECUSADA) {
            repositorioCandidaturas.alterarStatus(id, StatusCandidatura.RECUSADA);
            notificador.enviar(c.getIdPrestador(), TipoNotificacao.CANDIDATURA_RECUSADA,
                    "Sua candidatura para \"" + p.getTitulo() + "\" não foi aceita desta vez.", p.getId(), id);
            System.out.println("[AUDITORIA] Candidatura recusada: " + id);
        } else {
            throw new DadosInvalidosException("Use ACEITA (aceitar o candidato) ou RECUSADA (recusar o candidato).");
        }
        return repositorioCandidaturas.buscarPorId(id);
    }

    // AJUDA: aceita um só e o pedido passa para EM_ANDAMENTO. OFERTA: aceita quantos quiser, o pedido continua ABERTO.
    private void aceitar(Candidatura c, Pedido p) {
        if (p.getStatus() != StatusPedido.ABERTO) {
            throw new RegraNegocioException(p.ehOferta()
                    ? "Esta oferta não está mais aberta (status: " + p.getStatus() + ")."
                    : "Este pedido já tem um candidato aceito (status: " + p.getStatus() + ").");
        }
        if (p.getDtInicio() != null && !p.getDtInicio().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A data deste pedido já passou.");
        }
        if (!p.ehOferta() && !repositorioPedidos.marcarEmAndamento(p.getId())) {
            throw new RegraNegocioException("Este pedido acabou de ter um candidato aceito.");
        }
        repositorioCandidaturas.alterarStatus(c.getId(), StatusCandidatura.ACEITA);
        notificador.enviar(c.getIdPrestador(), TipoNotificacao.CANDIDATURA_ACEITA,
                "Sua candidatura para \"" + p.getTitulo() + "\" foi aceita.", p.getId(), c.getId());
        System.out.println("[AUDITORIA] Candidatura aceita: " + c.getId() + " pedido=" + p.getId());
    }

    // DELETE /candidaturas/{id}?usuarioId=&confirmar=true: o candidato desiste.
    // Precisa confirmar, porque não poderá se candidatar de novo a este pedido.
    // AJUDA: se era o aceito, o pedido volta para ABERTO e quem publicou pode aceitar outro da fila
    //        (se a data já passou, o pedido é CANCELADO).
    public void cancelar(String id, String usuarioId, Boolean confirmar) {
        Candidatura c = buscarExistente(id);
        c.validarPrestador(usuarioId);
        if (!c.ativa()) {
            throw new RegraNegocioException("Esta candidatura já está " + c.getStatus() + ".");
        }
        Pedido p = buscarPedido(c.getIdPedido());
        boolean eraAceita = c.aceita();
        if (eraAceita && p.getStatus() != StatusPedido.ABERTO && p.getStatus() != StatusPedido.EM_ANDAMENTO) {
            throw new RegraNegocioException("Não é possível desistir depois que o pedido foi concluído.");
        }
        if (!Boolean.TRUE.equals(confirmar)) {
            throw new RegraNegocioException("Ao desistir, você não poderá se candidatar a este pedido de novo. "
                    + "Para confirmar, envie confirmar=true.");
        }
        repositorioCandidaturas.alterarStatus(id, StatusCandidatura.CANCELADA);

        String mensagem;
        if (!eraAceita || p.ehOferta()) {
            mensagem = c.getNomePrestador() + (eraAceita ? " desistiu de \"" : " retirou a candidatura de \"")
                    + p.getTitulo() + "\".";
        } else if (p.getDtInicio() == null || p.getDtInicio().isAfter(LocalDateTime.now())) {
            repositorioPedidos.alterarStatus(p.getId(), StatusPedido.ABERTO);
            int naFila = repositorioCandidaturas.obterPendentes(p.getId()).size();
            mensagem = c.getNomePrestador() + " desistiu de \"" + p.getTitulo() + "\". O pedido voltou a ficar aberto"
                    + (naFila > 0 ? ": aceite outro candidato (" + naFila + " na fila)." : " para novas candidaturas.");
        } else {
            repositorioPedidos.alterarStatus(p.getId(), StatusPedido.CANCELADO);
            for (Candidatura pendente : repositorioCandidaturas.obterPendentes(p.getId())) {
                repositorioCandidaturas.alterarStatus(pendente.getId(), StatusCandidatura.RECUSADA);
            }
            mensagem = c.getNomePrestador() + " desistiu de \"" + p.getTitulo() + "\". Como a data já passou, o pedido foi cancelado.";
        }
        notificador.enviar(p.getIdSolicitante(), TipoNotificacao.CANDIDATURA_CANCELADA, mensagem, p.getId(), id);
        System.out.println("[AUDITORIA] Candidatura cancelada: " + id + " pedido=" + p.getId());
    }

    private Candidatura buscarExistente(String id) {
        Candidatura c = repositorioCandidaturas.buscarPorId(id);
        if (c == null) {
            throw new RecursoNaoEncontradoException("Candidatura não encontrada: " + id);
        }
        return c;
    }

    private Pedido buscarPedido(String idPedido) {
        Pedido p = repositorioPedidos.buscarPorId(idPedido);
        if (p == null) {
            throw new RecursoNaoEncontradoException("Pedido não encontrado: " + idPedido);
        }
        return p;
    }
}

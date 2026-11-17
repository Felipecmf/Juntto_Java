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
import br.com.juntto.model.StatusPagamento;
import br.com.juntto.model.StatusPedido;
import br.com.juntto.model.TipoNotificacao;
import br.com.juntto.model.Usuario;

import java.time.LocalDateTime;
import java.util.ArrayList;

// Candidatura: o prestador se oferece para o pedido e o solicitante escolhe um dos candidatos.
// Os não escolhidos ficam na fila (PENDENTE): se o escolhido desistir, o solicitante pode escolher outro.
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
        c.setStatusPagamento(StatusPagamento.PENDENTE);
        c.setDtOferta(LocalDateTime.now());
        repositorioCandidaturas.adicionar(c);
        notificador.enviar(p.getIdSolicitante(), TipoNotificacao.NOVA_CANDIDATURA,
                prestador.getNome() + " se candidatou ao seu pedido \"" + p.getTitulo() + "\".", idPedido, c.getId());
        System.out.println("[AUDITORIA] Candidatura: pedido=" + idPedido + " prestador=" + usuarioId);
        return repositorioCandidaturas.buscarPorId(c.getId());
    }

    // GET /candidaturas?usuarioId= (minhas candidaturas) ou todas, sem filtro
    public ArrayList<Candidatura> listar(String usuarioId) {
        if (usuarioId != null && !usuarioId.isBlank()) {
            return repositorioCandidaturas.obterPorUsuario(usuarioId);
        }
        return repositorioCandidaturas.obterTodas();
    }

    // GET /pedidos/{id}/candidaturas?usuarioId=: só o dono do pedido vê todos os candidatos
    public ArrayList<Candidatura> listarPorPedido(String idPedido, String usuarioId) {
        Pedido p = buscarPedido(idPedido);
        p.validarSolicitante(usuarioId);
        return repositorioCandidaturas.obterPorPedido(idPedido);
    }

    // GET /candidaturas/{id}?usuarioId=: o candidato ou o dono do pedido
    public Candidatura buscar(String id, String usuarioId) {
        Candidatura c = buscarExistente(id);
        c.validarVisualizacao(usuarioId);
        return c;
    }

    // PUT /candidaturas/{id}: o solicitante ESCOLHE um candidato (pedido vira ACEITO, os outros continuam na fila)
    // ou RECUSA um candidato pendente
    public Candidatura atualizar(String id, String usuarioId, StatusCandidatura novo) {
        Candidatura c = buscarExistente(id);
        Pedido p = buscarPedido(c.getIdPedido());
        p.validarSolicitante(usuarioId);
        if (c.getStatus() != StatusCandidatura.PENDENTE) {
            throw new RegraNegocioException("Só é possível alterar uma candidatura PENDENTE (esta está " + c.getStatus() + ").");
        }
        if (novo == StatusCandidatura.ESCOLHIDO) {
            escolher(c, p);
        } else if (novo == StatusCandidatura.RECUSADO) {
            repositorioCandidaturas.alterarStatus(id, StatusCandidatura.RECUSADO);
            notificador.enviar(c.getIdPrestador(), TipoNotificacao.CANDIDATURA_RECUSADA,
                    "Sua candidatura para \"" + p.getTitulo() + "\" não foi aceita desta vez.", p.getId(), id);
            System.out.println("[AUDITORIA] Candidatura recusada: " + id);
        } else {
            throw new DadosInvalidosException("Use ESCOLHIDO (escolher o candidato) ou RECUSADO (recusar o candidato).");
        }
        return repositorioCandidaturas.buscarPorId(id);
    }

    private void escolher(Candidatura c, Pedido p) {
        if (p.getStatus() != StatusPedido.ABERTO) {
            throw new RegraNegocioException("Este pedido já tem um prestador escolhido (status: " + p.getStatus() + ").");
        }
        if (p.getDtInicio() != null && !p.getDtInicio().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A data deste pedido já passou.");
        }
        if (!repositorioPedidos.marcarComoAceito(p.getId())) {
            throw new RegraNegocioException("Este pedido acabou de ter um prestador escolhido.");
        }
        repositorioCandidaturas.alterarStatus(c.getId(), StatusCandidatura.ESCOLHIDO);
        notificador.enviar(c.getIdPrestador(), TipoNotificacao.CANDIDATURA_ESCOLHIDA,
                "Você foi escolhido para o pedido \"" + p.getTitulo() + "\".", p.getId(), c.getId());
        System.out.println("[AUDITORIA] Candidatura escolhida: " + c.getId() + " pedido=" + p.getId());
    }

    // DELETE /candidaturas/{id}?usuarioId=&confirmar=true: o candidato desiste.
    // Precisa confirmar, porque não poderá se candidatar de novo a este pedido.
    // Se era o escolhido, o pedido volta para ABERTO e o solicitante pode escolher outro da fila.
    public void cancelar(String id, String usuarioId, Boolean confirmar) {
        Candidatura c = buscarExistente(id);
        c.validarPrestador(usuarioId);
        if (!c.ativa()) {
            throw new RegraNegocioException("Esta candidatura já está " + c.getStatus() + ".");
        }
        Pedido p = buscarPedido(c.getIdPedido());
        boolean eraEscolhido = c.getStatus() == StatusCandidatura.ESCOLHIDO;
        if (eraEscolhido && p.getStatus() != StatusPedido.ACEITO && p.getStatus() != StatusPedido.EM_ANDAMENTO) {
            throw new RegraNegocioException("Não é possível desistir depois de marcar o serviço como terminado.");
        }
        if (!Boolean.TRUE.equals(confirmar)) {
            throw new RegraNegocioException("Ao desistir, você não poderá se candidatar a este pedido de novo. "
                    + "Para confirmar, envie confirmar=true.");
        }
        repositorioCandidaturas.alterarStatus(id, StatusCandidatura.CANCELADO);

        String mensagem;
        if (!eraEscolhido) {
            mensagem = c.getNomePrestador() + " retirou a candidatura de \"" + p.getTitulo() + "\".";
        } else if (p.getDtInicio() == null || p.getDtInicio().isAfter(LocalDateTime.now())) {
            repositorioPedidos.alterarStatus(p.getId(), StatusPedido.ABERTO);
            int naFila = repositorioCandidaturas.obterPendentes(p.getId()).size();
            mensagem = c.getNomePrestador() + " desistiu de \"" + p.getTitulo() + "\". O pedido voltou a ficar aberto"
                    + (naFila > 0 ? ": escolha outro candidato (" + naFila + " na fila)." : " para novas candidaturas.");
        } else {
            repositorioPedidos.alterarStatus(p.getId(), StatusPedido.EXPIRADO);
            for (Candidatura pendente : repositorioCandidaturas.obterPendentes(p.getId())) {
                repositorioCandidaturas.alterarStatus(pendente.getId(), StatusCandidatura.RECUSADO);
            }
            mensagem = c.getNomePrestador() + " desistiu de \"" + p.getTitulo() + "\". Como a data já passou, o pedido expirou.";
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

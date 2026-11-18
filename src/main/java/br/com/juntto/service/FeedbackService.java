package br.com.juntto.service;

import br.com.juntto.dao.RepositorioCandidaturas;
import br.com.juntto.dao.RepositorioFeedbacks;
import br.com.juntto.dao.RepositorioPedidos;
import br.com.juntto.dto.FeedbackRequest;
import br.com.juntto.exception.AcessoNegadoException;
import br.com.juntto.exception.DadosInvalidosException;
import br.com.juntto.exception.RecursoNaoEncontradoException;
import br.com.juntto.exception.RegraNegocioException;
import br.com.juntto.model.Candidatura;
import br.com.juntto.model.Feedback;
import br.com.juntto.model.Pedido;
import br.com.juntto.model.StatusPedido;
import br.com.juntto.model.TipoNotificacao;

import java.time.LocalDateTime;
import java.util.ArrayList;

// Avaliação (tb_feedbacks): uma por candidatura aceita, com dois lados.
//  - quem recebeu a ajuda avalia quem ajudou -> estrelas_usu_ajudante / feedback_usu_ajudante
//  - quem ajudou avalia quem recebeu a ajuda -> estrelas_usu_pedido   / feedback_usu_pedido
// Em AJUDA quem ajuda é o candidato aceito; em OFERTA quem ajuda é quem publicou
// (e cada interessado aceito tem a sua própria avaliação).
public class FeedbackService {

    private RepositorioFeedbacks repositorioFeedbacks;
    private RepositorioPedidos repositorioPedidos;
    private RepositorioCandidaturas repositorioCandidaturas;
    private Notificador notificador;

    public FeedbackService(RepositorioFeedbacks repositorioFeedbacks, RepositorioPedidos repositorioPedidos,
                           RepositorioCandidaturas repositorioCandidaturas, Notificador notificador) {
        this.repositorioFeedbacks = repositorioFeedbacks;
        this.repositorioPedidos = repositorioPedidos;
        this.repositorioCandidaturas = repositorioCandidaturas;
        this.notificador = notificador;
    }

    // Regras: só os participantes (quem publicou e o candidato aceito), só em pedido concluído,
    // cada um avalia uma vez e só até 14 dias depois da conclusão
    public Feedback avaliar(String idPedido, FeedbackRequest pedido) {
        Pedido p = repositorioPedidos.buscarPorId(idPedido);
        if (p == null) {
            throw new RecursoNaoEncontradoException("Pedido não encontrado: " + idPedido);
        }
        String autor = pedido.getUsuarioId();
        Candidatura aceita = candidaturaAvaliada(p, autor, pedido.getIdCandidatura());
        if (p.getStatus() != StatusPedido.CONCLUIDO) {
            throw new RegraNegocioException("Só pedidos concluídos podem ser avaliados.");
        }
        if (p.getDtFinal() != null && LocalDateTime.now().isAfter(p.getDtFinal().plusDays(Feedback.DIAS_PARA_AVALIAR))) {
            throw new RegraNegocioException("O prazo de " + Feedback.DIAS_PARA_AVALIAR + " dias para avaliar este serviço terminou.");
        }

        Feedback f = repositorioFeedbacks.buscarPorCandidatura(aceita.getId());
        boolean novo = f == null;
        if (novo) {
            f = new Feedback();
            f.setIdCandidatura(aceita.getId());
        }
        String texto = limpar(pedido.getTexto());
        String avaliado;
        if (autor.equals(p.ajudadoDa(aceita))) {
            // quem recebeu a ajuda avalia quem ajudou
            if (f.getEstrelasAjudante() != null) {
                throw new RegraNegocioException("Você já avaliou este serviço. Use PUT /feedbacks/" + f.getId() + " para editar.");
            }
            f.setEstrelasAjudante(pedido.getEstrelas());
            f.setFeedbackAjudante(texto);
            f.setDtAvaliacaoAjudante(LocalDateTime.now());
            avaliado = p.ajudanteDa(aceita);
        } else {
            // quem ajudou avalia quem recebeu a ajuda
            if (f.getEstrelasAjudado() != null) {
                throw new RegraNegocioException("Você já avaliou este serviço. Use PUT /feedbacks/" + f.getId() + " para editar.");
            }
            f.setEstrelasAjudado(pedido.getEstrelas());
            f.setFeedbackAjudado(texto);
            f.setDtAvaliacaoAjudado(LocalDateTime.now());
            avaliado = p.ajudadoDa(aceita);
        }
        if (novo) {
            repositorioFeedbacks.adicionar(f);
        } else {
            repositorioFeedbacks.atualizar(f);
        }
        notificador.enviar(avaliado, TipoNotificacao.NOVO_FEEDBACK,
                "Você recebeu uma avaliação de " + pedido.getEstrelas() + " estrela(s) em \"" + p.getTitulo() + "\".",
                idPedido, aceita.getId());
        System.out.println("[AUDITORIA] Avaliação no feedback " + f.getId() + " por " + autor);
        return repositorioFeedbacks.buscarPorId(f.getId());
    }

    // Qual candidatura aceita está sendo avaliada:
    // - o candidato aceito avalia a própria candidatura
    // - quem publicou: em AJUDA a única aceita; em OFERTA com vários aceitos, informa idCandidatura
    private Candidatura candidaturaAvaliada(Pedido p, String autor, String idCandidatura) {
        ArrayList<Candidatura> aceitas = repositorioCandidaturas.obterAceitas(p.getId());
        for (Candidatura c : aceitas) {
            if (c.ehDoPrestador(autor)) {
                return c;
            }
        }
        if (!p.ehSolicitante(autor)) {
            throw new AcessoNegadoException("Somente quem recebeu e quem deu a ajuda neste serviço podem avaliá-lo.");
        }
        if (aceitas.isEmpty()) {
            throw new RegraNegocioException("Este pedido não tem candidato aceito para avaliar.");
        }
        if (idCandidatura == null || idCandidatura.isBlank()) {
            if (aceitas.size() > 1) {
                throw new DadosInvalidosException("Esta oferta tem " + aceitas.size()
                        + " interessados aceitos: informe o idCandidatura de quem você está avaliando.");
            }
            return aceitas.get(0);
        }
        for (Candidatura c : aceitas) {
            if (c.getId().equals(idCandidatura)) {
                return c;
            }
        }
        throw new RegraNegocioException("A candidatura " + idCandidatura + " não está aceita neste pedido.");
    }

    // usuarioId = avaliações recebidas pelo usuário (perfil) | nenhum = todas
    public ArrayList<Feedback> listar(String usuarioId) {
        if (usuarioId != null && !usuarioId.isBlank()) {
            return repositorioFeedbacks.obterRecebidos(usuarioId);
        }
        return repositorioFeedbacks.obterTodos();
    }

    public ArrayList<Feedback> listarPorPedido(String idPedido) {
        if (repositorioPedidos.buscarPorId(idPedido) == null) {
            throw new RecursoNaoEncontradoException("Pedido não encontrado: " + idPedido);
        }
        return repositorioFeedbacks.obterPorPedido(idPedido);
    }

    public Feedback buscar(String id) {
        Feedback f = repositorioFeedbacks.buscarPorId(id);
        if (f == null) {
            throw new RecursoNaoEncontradoException("Feedback não encontrado: " + id);
        }
        return f;
    }

    // Cada um edita só a própria avaliação, até 7 dias depois de avaliar
    public Feedback atualizar(String id, FeedbackRequest pedido) {
        Feedback f = buscar(id);
        String autor = pedido.getUsuarioId();
        f.validarParticipante(autor);
        if (f.autorEhAjudado(autor)) {
            f.validarPrazoDeEdicao(f.getDtAvaliacaoAjudante());
            f.setEstrelasAjudante(pedido.getEstrelas());
            f.setFeedbackAjudante(limpar(pedido.getTexto()));
        } else {
            f.validarPrazoDeEdicao(f.getDtAvaliacaoAjudado());
            f.setEstrelasAjudado(pedido.getEstrelas());
            f.setFeedbackAjudado(limpar(pedido.getTexto()));
        }
        repositorioFeedbacks.atualizar(f);
        System.out.println("[AUDITORIA] Avaliação editada no feedback " + id + " por " + autor);
        return repositorioFeedbacks.buscarPorId(id);
    }

    // Apaga só a avaliação de quem pediu a remoção; se os dois lados ficarem vazios, a linha é removida
    public void remover(String id, String usuarioId) {
        Feedback f = buscar(id);
        f.validarParticipante(usuarioId);
        if (f.autorEhAjudado(usuarioId)) {
            f.validarPrazoDeEdicao(f.getDtAvaliacaoAjudante());
            f.setEstrelasAjudante(null);
            f.setFeedbackAjudante(null);
            f.setDtAvaliacaoAjudante(null);
        } else {
            f.validarPrazoDeEdicao(f.getDtAvaliacaoAjudado());
            f.setEstrelasAjudado(null);
            f.setFeedbackAjudado(null);
            f.setDtAvaliacaoAjudado(null);
        }
        if (f.vazio()) {
            repositorioFeedbacks.remover(id);
        } else {
            repositorioFeedbacks.atualizar(f);
        }
        System.out.println("[AUDITORIA] Avaliação removida do feedback " + id + " por " + usuarioId);
    }

    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

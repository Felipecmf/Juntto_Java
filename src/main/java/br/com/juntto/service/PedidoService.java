package br.com.juntto.service;

import br.com.juntto.dao.RepositorioCandidaturas;
import br.com.juntto.dao.RepositorioCategorias;
import br.com.juntto.dao.RepositorioPedidos;
import br.com.juntto.dao.RepositorioUsuarios;
import br.com.juntto.dto.PagamentoRequest;
import br.com.juntto.dto.StatusPedidoRequest;
import br.com.juntto.exception.DadosInvalidosException;
import br.com.juntto.exception.RecursoNaoEncontradoException;
import br.com.juntto.exception.RegraNegocioException;
import br.com.juntto.model.Candidatura;
import br.com.juntto.model.Pedido;
import br.com.juntto.model.StatusCandidatura;
import br.com.juntto.model.StatusPagamento;
import br.com.juntto.model.StatusPedido;
import br.com.juntto.model.TipoNotificacao;
import br.com.juntto.model.TipoPublicacao;
import br.com.juntto.model.Usuario;
import br.com.juntto.util.Validador;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class PedidoService {

    private static final String TIPO_PUBLICACAO_PADRAO = "AJUDA";

    private RepositorioPedidos repositorioPedidos;
    private RepositorioCandidaturas repositorioCandidaturas;
    private RepositorioUsuarios repositorioUsuarios;
    private RepositorioCategorias repositorioCategorias;
    private Notificador notificador;

    public PedidoService(RepositorioPedidos repositorioPedidos, RepositorioCandidaturas repositorioCandidaturas,
                         RepositorioUsuarios repositorioUsuarios, RepositorioCategorias repositorioCategorias,
                         Notificador notificador) {
        this.repositorioPedidos = repositorioPedidos;
        this.repositorioCandidaturas = repositorioCandidaturas;
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioCategorias = repositorioCategorias;
        this.notificador = notificador;
    }

    // ============================== CRUD ==============================

    // Regra: todo pedido nasce Aberto
    public Pedido criar(Pedido pedido) {
        Usuario solicitante = buscarUsuario(pedido.getIdSolicitante());
        validarDados(pedido);
        pedido.setStatus(StatusPedido.ABERTO);
        repositorioPedidos.adicionar(pedido);
        System.out.println("[AUDITORIA] Pedido publicado: " + pedido.getId() + " solicitante=" + solicitante.getId());
        return repositorioPedidos.buscarPorId(pedido.getId());
    }

    // Lista de oportunidades: só pedidos Abertos
    public ArrayList<Pedido> listarAbertos(String tipo, String idCategoria, Double valorMin, Double valorMax, LocalDate data) {
        verificarPrazos();
        if (valorMin != null && valorMax != null && valorMin > valorMax) {
            throw new DadosInvalidosException("valorMin não pode ser maior que valorMax.");
        }
        String tipoFiltro = tipo == null || tipo.isBlank() ? null : tipoValido(tipo);
        ArrayList<Pedido> pedidos = repositorioPedidos.obterAbertos(tipoFiltro, idCategoria, valorMin, valorMax, data);
        for (Pedido p : pedidos) {
            esconderContatos(p);
        }
        return pedidos;
    }

    // usuarioId é opcional: se for o solicitante ou o prestador escolhido, os e-mails de contato aparecem
    public Pedido buscar(String id, String usuarioId) {
        verificarPrazos();
        Pedido p = buscarExistente(id);
        if (!p.ehSolicitante(usuarioId) && !p.ehPrestador(usuarioId)) {
            esconderContatos(p);
        }
        return p;
    }

    // Histórico: SOLICITANTE = pedidos que publiquei | PRESTADOR = pedidos em que me candidatei
    public ArrayList<Pedido> listarDoUsuario(String usuarioId, String papel) {
        verificarPrazos();
        Usuario u = repositorioUsuarios.buscarPorId(usuarioId);
        if (u == null || u.removido()) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + usuarioId);
        }
        ArrayList<Pedido> pedidos;
        if (papel == null || papel.equalsIgnoreCase("SOLICITANTE")) {
            pedidos = repositorioPedidos.obterPorSolicitante(usuarioId);
        } else if (papel.equalsIgnoreCase("PRESTADOR")) {
            pedidos = repositorioPedidos.obterPorPrestador(usuarioId);
        } else {
            throw new DadosInvalidosException("O campo 'papel' deve ser SOLICITANTE ou PRESTADOR.");
        }
        for (Pedido p : pedidos) {
            if (!p.ehSolicitante(usuarioId) && !p.ehPrestador(usuarioId)) {
                esconderContatos(p);
            }
        }
        return pedidos;
    }

    public Pedido atualizar(String id, Pedido novo) {
        Pedido existente = buscarExistente(id);
        existente.validarSolicitante(novo.getIdSolicitante());
        existente.validarEditavel();
        validarDados(novo);
        novo.setId(id);
        repositorioPedidos.atualizar(novo);
        System.out.println("[AUDITORIA] Pedido atualizado: " + id);
        return repositorioPedidos.buscarPorId(id);
    }

    // Só enquanto Aberto: sem candidaturas o pedido é apagado; com candidaturas, é cancelado
    public void remover(String id, String usuarioId) {
        Pedido p = buscarExistente(id);
        p.validarSolicitante(usuarioId);
        p.validarEditavel();
        if (repositorioPedidos.possuiVinculos(id)) {
            repositorioPedidos.alterarStatus(id, StatusPedido.CANCELADO);
            recusarPendentes(p, "foi cancelado pelo solicitante");
            System.out.println("[AUDITORIA] Pedido cancelado: " + id);
        } else {
            repositorioPedidos.remover(id);
            System.out.println("[AUDITORIA] Pedido excluído: " + id);
        }
    }

    // ============================== ANDAMENTO ==============================

    // Aberto -> Aceito -> Em andamento -> Aguardando confirmação -> Concluído (-> Finalizado após o pagamento)
    // EM_ANDAMENTO (quem ajuda inicia) | AGUARDANDO_CONFIRMACAO (quem ajuda terminou)
    // CONCLUIDO (quem recebeu a ajuda confirma) | CANCELADO (quem publicou, antes de iniciar)
    // Em AJUDA quem ajuda é o candidato escolhido; em OFERTA quem ajuda é quem publicou.
    // O candidato desiste pelo DELETE /candidaturas/{id}
    public Pedido alterarStatus(String id, StatusPedidoRequest pedido) {
        String usuarioId = pedido.getUsuarioId();
        StatusPedido novo = pedido.getStatus();
        Pedido p = buscarExistente(id);
        String candidatura = p.getIdCandidaturaEscolhida();

        if (novo == StatusPedido.EM_ANDAMENTO) {
            p.validarAjudante(usuarioId);
            p.validarStatus(StatusPedido.ACEITO, "iniciar o serviço");
            repositorioPedidos.alterarStatus(id, StatusPedido.EM_ANDAMENTO);
            notificador.enviar(p.getIdAjudado(), TipoNotificacao.SERVICO_INICIADO,
                    p.nomeDoAjudante() + " iniciou o serviço \"" + p.getTitulo() + "\".", id, candidatura);
        } else if (novo == StatusPedido.AGUARDANDO_CONFIRMACAO) {
            p.validarAjudante(usuarioId);
            p.validarStatus(StatusPedido.EM_ANDAMENTO, "marcar como terminado");
            repositorioPedidos.alterarStatus(id, StatusPedido.AGUARDANDO_CONFIRMACAO);
            notificador.enviar(p.getIdAjudado(), TipoNotificacao.SERVICO_TERMINADO,
                    p.nomeDoAjudante() + " terminou o serviço \"" + p.getTitulo() + "\". Confirme a conclusão.", id, candidatura);
        } else if (novo == StatusPedido.CONCLUIDO) {
            concluir(p, usuarioId);
        } else if (novo == StatusPedido.CANCELADO) {
            cancelarPeloSolicitante(p, usuarioId);
        } else {
            throw new DadosInvalidosException("Use EM_ANDAMENTO, AGUARDANDO_CONFIRMACAO, CONCLUIDO ou CANCELADO.");
        }
        System.out.println("[AUDITORIA] Pedido " + id + " -> " + novo + " por " + usuarioId);
        return repositorioPedidos.buscarPorId(id);
    }

    // Quem recebeu a ajuda confirma a conclusão. A data/hora real do fim é gravada em dt_final.
    // Publicação sem pagamento (naoIncluirPagamento, e toda OFERTA) já termina FINALIZADA.
    private void concluir(Pedido p, String usuarioId) {
        p.validarAjudado(usuarioId);
        p.validarStatus(StatusPedido.AGUARDANDO_CONFIRMACAO, "confirmar a conclusão");
        StatusPedido status = p.semPagamento() ? StatusPedido.FINALIZADO : StatusPedido.CONCLUIDO;
        repositorioPedidos.concluir(p.getId(), status, LocalDateTime.now());
        recusarPendentes(p, "foi concluído com outro prestador");
        String mensagem = p.semPagamento()
                ? "A conclusão de \"" + p.getTitulo() + "\" foi confirmada. Ajuda finalizada, obrigado!"
                : "A conclusão de \"" + p.getTitulo() + "\" foi confirmada. Aguarde o registro do pagamento.";
        notificador.enviar(p.getIdAjudante(), TipoNotificacao.PEDIDO_CONCLUIDO, mensagem, p.getId(), p.getIdCandidaturaEscolhida());
    }

    // Regra: quem publicou só cancela antes de o serviço começar (Aberto ou Aceito)
    private void cancelarPeloSolicitante(Pedido p, String usuarioId) {
        p.validarSolicitante(usuarioId);
        if (p.getStatus() != StatusPedido.ABERTO && p.getStatus() != StatusPedido.ACEITO) {
            throw new RegraNegocioException("O pedido só pode ser cancelado antes de o serviço começar.");
        }
        if (p.getStatus() == StatusPedido.ACEITO) {
            repositorioCandidaturas.alterarStatus(p.getIdCandidaturaEscolhida(), StatusCandidatura.RECUSADO);
            notificador.enviar(p.getIdPrestador(), TipoNotificacao.PEDIDO_CANCELADO,
                    "O pedido \"" + p.getTitulo() + "\" foi cancelado pelo solicitante.", p.getId(), p.getIdCandidaturaEscolhida());
        }
        repositorioPedidos.alterarStatus(p.getId(), StatusPedido.CANCELADO);
        recusarPendentes(p, "foi cancelado pelo solicitante");
    }

    // ============================== PAGAMENTO ==============================

    // Regra: só depois que a conclusão é confirmada. Quem recebeu a ajuda registra (OFERTA nunca tem pagamento).
    // A caixinha e a data ficam no pedido (caixinha, dt_recompensa); o status fica na candidatura escolhida.
    public Pedido registrarPagamento(String id, PagamentoRequest pedido) {
        Pedido p = buscarExistente(id);
        p.validarAjudado(pedido.getUsuarioId());
        if (p.semPagamento()) {
            throw new RegraNegocioException("Esta publicação não tem pagamento.");
        }
        p.validarStatus(StatusPedido.CONCLUIDO, "registrar o pagamento");
        if (p.getStatusPagamento() != StatusPagamento.PENDENTE) {
            throw new RegraNegocioException("O pagamento deste pedido já foi registrado.");
        }
        double caixinha = pedido.getCaixinha() == null ? 0 : pedido.getCaixinha();
        repositorioPedidos.registrarPagamento(id, caixinha, LocalDateTime.now());
        repositorioCandidaturas.alterarStatusPagamento(p.getIdCandidaturaEscolhida(), StatusPagamento.REGISTRADO, null);
        double total = (p.getValor() == null ? 0 : p.getValor()) + caixinha;
        notificador.enviar(p.getIdAjudante(), TipoNotificacao.PAGAMENTO_REGISTRADO,
                "Pagamento de R$ " + String.format("%.2f", total) + " registrado em \"" + p.getTitulo()
                        + "\". Confirme o recebimento.", id, p.getIdCandidaturaEscolhida());
        System.out.println("[AUDITORIA] Pagamento registrado: pedido=" + id + " total=" + total);
        return repositorioPedidos.buscarPorId(id);
    }

    // Quem ajudou confirma que recebeu: a ajuda é finalizada
    public Pedido confirmarPagamento(String id, String usuarioId) {
        Pedido p = buscarExistente(id);
        p.validarAjudante(usuarioId);
        if (p.getStatus() != StatusPedido.CONCLUIDO || p.getStatusPagamento() != StatusPagamento.REGISTRADO) {
            throw new RegraNegocioException("Não há pagamento aguardando confirmação neste pedido.");
        }
        finalizar(p, false);
        return repositorioPedidos.buscarPorId(id);
    }

    // ============================== PRAZOS ==============================

    // Chamado sempre que pedidos ou notificações são consultados:
    // 1) pedido Aberto com a data vencida vira EXPIRADO (e a fila de candidatos é encerrada)
    // 2) pagamento sem confirmação do prestador em 48h é confirmado automaticamente
    public void verificarPrazos() {
        LocalDateTime agora = LocalDateTime.now();
        for (Pedido p : repositorioPedidos.obterAbertosVencidos(agora)) {
            repositorioPedidos.alterarStatus(p.getId(), StatusPedido.EXPIRADO);
            recusarPendentes(p, "expirou");
        }
        for (Pedido p : repositorioPedidos.obterComPagamentoRegistrado()) {
            if (p.prazoDePagamentoVencido(agora)) {
                finalizar(p, true);
            }
        }
    }

    // ============================== AUXILIARES ==============================

    private void finalizar(Pedido p, boolean automatico) {
        repositorioCandidaturas.alterarStatusPagamento(p.getIdCandidaturaEscolhida(), StatusPagamento.CONFIRMADO, LocalDateTime.now());
        repositorioPedidos.alterarStatus(p.getId(), StatusPedido.FINALIZADO);
        String mensagem = automatico
                ? "O pagamento de \"" + p.getTitulo() + "\" foi confirmado automaticamente (sem resposta em 48h). Ajuda finalizada."
                : p.nomeDoAjudante() + " confirmou o recebimento do pagamento de \"" + p.getTitulo() + "\". Ajuda finalizada.";
        notificador.enviar(p.getIdAjudado(), TipoNotificacao.PAGAMENTO_CONFIRMADO, mensagem, p.getId(), p.getIdCandidaturaEscolhida());
        System.out.println("[AUDITORIA] Ajuda finalizada: " + p.getId() + (automatico ? " (automático)" : ""));
    }

    // Fim da fila: quem ainda estava PENDENTE vira RECUSADO e recebe aviso
    private void recusarPendentes(Pedido p, String motivo) {
        for (Candidatura pendente : repositorioCandidaturas.obterPendentes(p.getId())) {
            repositorioCandidaturas.alterarStatus(pendente.getId(), StatusCandidatura.RECUSADO);
            notificador.enviar(pendente.getIdPrestador(), TipoNotificacao.CANDIDATURA_RECUSADA,
                    "O pedido \"" + p.getTitulo() + "\" " + motivo + ". Sua candidatura foi encerrada.", p.getId(), pendente.getId());
        }
    }

    // Regras do formulário: tipo AJUDA ou OFERTA, categoria existente, data futura, data final depois do início,
    // valor obrigatório quando há pagamento; OFERTA é sempre sem pagamento
    private void validarDados(Pedido p) {
        String tipo = p.getTipoPublicacao() == null || p.getTipoPublicacao().isBlank()
                ? TIPO_PUBLICACAO_PADRAO : tipoValido(p.getTipoPublicacao());
        p.setTipoPublicacao(tipo);
        if (p.ehOferta()) {
            if (p.getValor() != null) {
                throw new DadosInvalidosException("Oferta de ajuda não tem pagamento: não envie valor.");
            }
            p.setNaoIncluirPagamento(true);
        }
        if (repositorioCategorias.buscarPorId(p.getIdCategoria()) == null) {
            throw new DadosInvalidosException("Categoria não encontrada: " + p.getIdCategoria());
        }
        Validador.dataFutura(p.getDtInicio(), "dtInicio");
        if (p.getDtFinal() != null && !p.getDtFinal().isAfter(p.getDtInicio())) {
            throw new DadosInvalidosException("A data final precisa ser depois da data de início.");
        }
        if (p.semPagamento()) {
            p.setValor(null);
        } else if (p.getValor() == null) {
            throw new DadosInvalidosException("Informe o valor ou marque naoIncluirPagamento = true.");
        }
        p.setNaoIncluirPagamento(p.semPagamento());
        p.setRecompensa(Validador.textoOpcional(p.getRecompensa(), "recompensa", 300));
        p.setFotoUrl(Validador.textoOpcional(p.getFotoUrl(), "fotoUrl", 200));
    }

    private static String tipoValido(String tipo) {
        String t = tipo.trim().toUpperCase();
        for (TipoPublicacao valido : TipoPublicacao.values()) {
            if (valido.name().equals(t)) {
                return t;
            }
        }
        throw new DadosInvalidosException("O tipo de publicação deve ser AJUDA ou OFERTA.");
    }

    // Sem chat e sem telefone no banco: o contato é o e-mail, visível só para os dois participantes
    private void esconderContatos(Pedido p) {
        p.setEmailSolicitante(null);
        p.setEmailPrestador(null);
    }

    private Pedido buscarExistente(String id) {
        Pedido p = repositorioPedidos.buscarPorId(id);
        if (p == null) {
            throw new RecursoNaoEncontradoException("Pedido não encontrado: " + id);
        }
        return p;
    }

    private Usuario buscarUsuario(String usuarioId) {
        Usuario u = repositorioUsuarios.buscarPorId(usuarioId);
        if (u == null || u.removido()) {
            throw new DadosInvalidosException("Usuário não encontrado: " + usuarioId);
        }
        return u;
    }
}

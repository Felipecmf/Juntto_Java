package br.com.juntto.service;

import br.com.juntto.dao.RepositorioCandidaturas;
import br.com.juntto.dao.RepositorioCategorias;
import br.com.juntto.dao.RepositorioPedidos;
import br.com.juntto.dao.RepositorioUsuarios;
import br.com.juntto.dto.PagamentoRequest;
import br.com.juntto.dto.StatusPedidoRequest;
import br.com.juntto.exception.AcessoNegadoException;
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

    // usuarioId é opcional: os e-mails de contato só aparecem para os participantes
    // (quem publicou e quem tem candidatura aceita)
    public Pedido buscar(String id, String usuarioId) {
        verificarPrazos();
        Pedido p = buscarExistente(id);
        if (!participa(p, usuarioId)) {
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
            if (!participa(p, usuarioId)) {
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
        // quem ainda está na fila acompanha a mudança entre "com" e "sem" pagamento
        repositorioCandidaturas.alterarStatusPagamentoDasPendentes(id, statusPagamentoInicial(novo));
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
            recusarPendentes(p, "foi cancelado por quem publicou");
            System.out.println("[AUDITORIA] Pedido cancelado: " + id);
        } else {
            repositorioPedidos.remover(id);
            System.out.println("[AUDITORIA] Pedido excluído: " + id);
        }
    }

    // ============================== ANDAMENTO ==============================

    // Status do documento de banco: ABERTO, EM_ANDAMENTO, CONCLUIDO e CANCELADO.
    // AJUDA:  ABERTO -> EM_ANDAMENTO (automático, quando quem publicou aceita um candidato) -> CONCLUIDO
    // OFERTA: ABERTO (aceita vários interessados) -> CONCLUIDO
    // Por esta rota: CONCLUIDO (em AJUDA quem recebeu a ajuda; em OFERTA quem ofereceu) ou CANCELADO (quem publicou).
    // O candidato desiste pelo DELETE /candidaturas/{id}.
    public Pedido alterarStatus(String id, StatusPedidoRequest pedido) {
        String usuarioId = pedido.getUsuarioId();
        StatusPedido novo = pedido.getStatus();
        Pedido p = buscarExistente(id);
        if (novo == StatusPedido.CONCLUIDO) {
            concluir(p, usuarioId, pedido.getCaixinha());
        } else if (novo == StatusPedido.CANCELADO) {
            cancelarPeloSolicitante(p, usuarioId);
        } else {
            throw new DadosInvalidosException("Use CONCLUIDO ou CANCELADO. EM_ANDAMENTO é automático quando um candidato é aceito.");
        }
        System.out.println("[AUDITORIA] Pedido " + id + " -> " + novo + " por " + usuarioId);
        return repositorioPedidos.buscarPorId(id);
    }

    // AJUDA: quem recebeu a ajuda (quem publicou) confirma, com o pedido EM_ANDAMENTO, e pode informar a caixinha.
    // OFERTA: quem ofereceu encerra a oferta, que precisa ter pelo menos um interessado aceito.
    // A data/hora real do fim é gravada em dt_final; quem ainda estava na fila é recusado.
    private void concluir(Pedido p, String usuarioId, Double caixinha) {
        p.validarSolicitante(usuarioId);
        ArrayList<Candidatura> aceitas = repositorioCandidaturas.obterAceitas(p.getId());
        if (p.ehOferta()) {
            p.validarStatus(StatusPedido.ABERTO, "encerrar a oferta");
            if (aceitas.isEmpty()) {
                throw new RegraNegocioException("Aceite pelo menos um interessado antes de concluir a oferta (ou cancele).");
            }
            if (caixinha != null) {
                throw new DadosInvalidosException("Caixinha só existe ao concluir um pedido de AJUDA.");
            }
        } else {
            p.validarStatus(StatusPedido.EM_ANDAMENTO, "concluir o pedido");
            if (caixinha != null && p.semPagamento()) {
                throw new DadosInvalidosException("Este pedido não tem pagamento: não envie caixinha.");
            }
        }
        repositorioPedidos.concluir(p.getId(), LocalDateTime.now(), caixinha);
        recusarPendentes(p, "foi concluído");
        for (Candidatura c : aceitas) {
            String destino = p.ehOferta() ? c.getIdPrestador() : p.ajudanteDa(c);
            String mensagem = "\"" + p.getTitulo() + "\" foi concluído."
                    + (c.getStatusPagamento() == StatusPagamento.PENDENTE
                    ? (p.ehOferta() ? " Combine o pagamento com quem ofereceu." : " Confirme quando receber o pagamento.")
                    : " Obrigado pela ajuda!");
            notificador.enviar(destino, TipoNotificacao.PEDIDO_CONCLUIDO, mensagem, p.getId(), c.getId());
        }
    }

    // Quem publicou cancela enquanto o pedido não foi concluído. Candidaturas aceitas viram CANCELADA (com aviso).
    private void cancelarPeloSolicitante(Pedido p, String usuarioId) {
        p.validarSolicitante(usuarioId);
        if (p.getStatus() != StatusPedido.ABERTO && p.getStatus() != StatusPedido.EM_ANDAMENTO) {
            throw new RegraNegocioException("O pedido só pode ser cancelado antes de ser concluído.");
        }
        for (Candidatura aceita : repositorioCandidaturas.obterAceitas(p.getId())) {
            repositorioCandidaturas.alterarStatus(aceita.getId(), StatusCandidatura.CANCELADA);
            notificador.enviar(aceita.getIdPrestador(), TipoNotificacao.PEDIDO_CANCELADO,
                    "O pedido \"" + p.getTitulo() + "\" foi cancelado por quem publicou.", p.getId(), aceita.getId());
        }
        repositorioPedidos.alterarStatus(p.getId(), StatusPedido.CANCELADO);
        recusarPendentes(p, "foi cancelado por quem publicou");
    }

    // ============================== PAGAMENTO ==============================

    // O pagamento acontece fora do app: o banco só registra a confirmação de quem RECEBEU.
    // AJUDA: quem ajudou (candidato aceito) confirma, depois da conclusão.
    // OFERTA: quem ofereceu confirma, um interessado aceito por vez (idCandidatura quando há mais de um).
    // Pedido sem pagamento com recompensa: quem ajudou confirma que recebeu a recompensa (dt_recompensa).
    public Pedido confirmarPagamento(String id, PagamentoRequest pedido) {
        String usuarioId = pedido.getUsuarioId();
        Pedido p = buscarExistente(id);
        Candidatura c = escolherAceita(p, pedido.getIdCandidatura());
        if (!usuarioId.equals(p.ajudanteDa(c))) {
            throw new AcessoNegadoException("Somente quem recebeu o pagamento (quem ajudou ou ofereceu) pode confirmar.");
        }
        if (!p.ehOferta()) {
            p.validarStatus(StatusPedido.CONCLUIDO, "confirmar o recebimento");
        } else if (p.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Esta oferta foi cancelada.");
        }

        if (p.semPagamento()) {
            if (p.getRecompensa() == null || p.ehOferta()) {
                throw new RegraNegocioException("Este pedido não tem pagamento para confirmar.");
            }
            if (p.getDtRecompensa() != null) {
                throw new RegraNegocioException("O recebimento da recompensa já foi confirmado.");
            }
            repositorioPedidos.confirmarRecompensa(id, LocalDateTime.now());
            notificador.enviar(p.ajudadoDa(c), TipoNotificacao.RECOMPENSA_CONFIRMADA,
                    p.nomeDoAjudante() + " confirmou que recebeu a recompensa de \"" + p.getTitulo() + "\".", id, c.getId());
        } else {
            if (c.getStatusPagamento() == StatusPagamento.CONFIRMADO) {
                throw new RegraNegocioException("O pagamento desta candidatura já foi confirmado.");
            }
            confirmar(p, c, false);
        }
        return repositorioPedidos.buscarPorId(id);
    }

    // ============================== PRAZOS ==============================

    // Chamado sempre que pedidos ou notificações são consultados:
    // 1) pedido Aberto com a data vencida e sem ninguém aceito vira CANCELADO (e a fila é encerrada)
    // 2) pagamento sem confirmação 48h depois da conclusão é confirmado automaticamente
    public void verificarPrazos() {
        LocalDateTime agora = LocalDateTime.now();
        for (Pedido p : repositorioPedidos.obterAbertosVencidos(agora)) {
            repositorioPedidos.alterarStatus(p.getId(), StatusPedido.CANCELADO);
            recusarPendentes(p, "passou da data e foi encerrado");
        }
        for (Candidatura c : repositorioCandidaturas.obterPagamentosPendentesAte(agora.minusHours(Pedido.HORAS_PARA_CONFIRMAR_PAGAMENTO))) {
            Pedido p = repositorioPedidos.buscarPorId(c.getIdPedido());
            confirmar(p, c, true);
        }
    }

    // ============================== AUXILIARES ==============================

    private void confirmar(Pedido p, Candidatura c, boolean automatico) {
        repositorioCandidaturas.alterarStatusPagamento(c.getId(), StatusPagamento.CONFIRMADO, LocalDateTime.now());
        String mensagem = automatico
                ? "O pagamento de \"" + p.getTitulo() + "\" foi confirmado automaticamente (sem resposta em 48h)."
                : "O recebimento do pagamento de \"" + p.getTitulo() + "\" foi confirmado.";
        notificador.enviar(p.ajudadoDa(c), TipoNotificacao.PAGAMENTO_CONFIRMADO, mensagem, p.getId(), c.getId());
        System.out.println("[AUDITORIA] Pagamento confirmado: candidatura " + c.getId() + (automatico ? " (automático)" : ""));
    }

    // Candidatura aceita usada na confirmação: em AJUDA é a única; em OFERTA com vários, idCandidatura diz qual
    private Candidatura escolherAceita(Pedido p, String idCandidatura) {
        ArrayList<Candidatura> aceitas = repositorioCandidaturas.obterAceitas(p.getId());
        if (aceitas.isEmpty()) {
            throw new RegraNegocioException("Este pedido não tem candidatura aceita.");
        }
        if (idCandidatura == null || idCandidatura.isBlank()) {
            if (aceitas.size() > 1) {
                throw new DadosInvalidosException("Esta oferta tem " + aceitas.size()
                        + " interessados aceitos: informe o idCandidatura.");
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

    // Participante: quem publicou ou quem tem candidatura aceita no pedido
    private boolean participa(Pedido p, String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            return false;
        }
        if (p.ehSolicitante(usuarioId) || p.ehPrestador(usuarioId)) {
            return true;
        }
        if (!p.ehOferta() || p.getTotalAceitas() == null || p.getTotalAceitas() == 0) {
            return false;
        }
        for (Candidatura c : repositorioCandidaturas.obterAceitas(p.getId())) {
            if (c.ehDoPrestador(usuarioId)) {
                return true;
            }
        }
        return false;
    }

    // Fim da fila: quem ainda estava PENDENTE vira RECUSADA e recebe aviso
    private void recusarPendentes(Pedido p, String motivo) {
        for (Candidatura pendente : repositorioCandidaturas.obterPendentes(p.getId())) {
            repositorioCandidaturas.alterarStatus(pendente.getId(), StatusCandidatura.RECUSADA);
            notificador.enviar(pendente.getIdPrestador(), TipoNotificacao.CANDIDATURA_RECUSADA,
                    "O pedido \"" + p.getTitulo() + "\" " + motivo + ". Sua candidatura foi encerrada.", p.getId(), pendente.getId());
        }
    }

    // Pedido sem pagamento: as candidaturas nascem NAO_APLICAVEL; com pagamento, PENDENTE
    public static StatusPagamento statusPagamentoInicial(Pedido p) {
        return p.semPagamento() ? StatusPagamento.NAO_APLICAVEL : StatusPagamento.PENDENTE;
    }

    // Regras do formulário: tipo AJUDA ou OFERTA, categoria existente, data futura, data final depois do início,
    // valor obrigatório quando há pagamento (vale para AJUDA e para OFERTA, ex.: produto à venda)
    private void validarDados(Pedido p) {
        String tipo = p.getTipoPublicacao() == null || p.getTipoPublicacao().isBlank()
                ? TIPO_PUBLICACAO_PADRAO : tipoValido(p.getTipoPublicacao());
        p.setTipoPublicacao(tipo);
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

    // Sem chat e sem telefone no banco: o contato é o e-mail, visível só para os participantes
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

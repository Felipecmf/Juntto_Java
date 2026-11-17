package br.com.juntto.dao;

import br.com.juntto.model.Pedido;
import br.com.juntto.model.StatusPedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public interface RepositorioPedidos {

    void adicionar(Pedido pedido);

    boolean atualizar(Pedido pedido);

    Pedido buscarPorId(String id);

    ArrayList<Pedido> obterAbertos(String tipo, String idCategoria, Double valorMin, Double valorMax, LocalDate data);

    ArrayList<Pedido> obterPorSolicitante(String usuarioId);

    ArrayList<Pedido> obterPorPrestador(String usuarioId);

    ArrayList<Pedido> obterAbertosVencidos(LocalDateTime agora);

    ArrayList<Pedido> obterComPagamentoRegistrado();

    boolean possuiVinculos(String id);

    int contarAtivosDoUsuario(String usuarioId);

    boolean remover(String id);

    // Só marca como aceito se ainda estiver Aberto (duas escolhas ao mesmo tempo: só uma vence)
    boolean marcarComoAceito(String id);

    void alterarStatus(String id, StatusPedido status);

    // Conclusão confirmada: grava a data/hora final real do serviço (dt_final e hora_final)
    void concluir(String id, StatusPedido status, LocalDateTime quando);

    // Pagamento registrado: grava a caixinha e a data em dt_recompensa
    void registrarPagamento(String id, double caixinha, LocalDateTime quando);
}

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

    boolean possuiVinculos(String id);

    int contarAtivosDoUsuario(String usuarioId);

    boolean remover(String id);

    // AJUDA: só passa para EM_ANDAMENTO se ainda estiver Aberto (dois aceites ao mesmo tempo: só um vence)
    boolean marcarEmAndamento(String id);

    void alterarStatus(String id, StatusPedido status);

    // Conclusão: status CONCLUIDO, data/hora final real (dt_final e hora_final) e a caixinha, se houver
    void concluir(String id, LocalDateTime quando, Double caixinha);

    // Quem ajudou confirmou que recebeu a recompensa (pedido sem pagamento): grava dt_recompensa
    void confirmarRecompensa(String id, LocalDateTime quando);
}

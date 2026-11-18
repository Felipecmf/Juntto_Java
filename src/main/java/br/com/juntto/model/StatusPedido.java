package br.com.juntto.model;

// Valores do CHECK tb_ped_sta_ck (documento de banco do grupo).
// AJUDA:  ABERTO -> EM_ANDAMENTO (candidato aceito) -> CONCLUIDO
// OFERTA: ABERTO (aceita vários interessados) -> CONCLUIDO
// Qualquer um pode terminar em CANCELADO.
public enum StatusPedido {
    ABERTO, EM_ANDAMENTO, CONCLUIDO, CANCELADO
}

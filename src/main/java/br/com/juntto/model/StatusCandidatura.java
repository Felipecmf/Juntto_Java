package br.com.juntto.model;

// Valores do CHECK tb_can_ped_sta_ck (documento de banco do grupo).
// PENDENTE: na fila | ACEITA: participa do pedido | RECUSADA: não foi aceita | CANCELADA: o candidato desistiu
public enum StatusCandidatura {
    PENDENTE, ACEITA, RECUSADA, CANCELADA
}

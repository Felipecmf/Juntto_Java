package br.com.juntto.model;

// Valores do CHECK tb_can_ped_sta_pag_ck (documento de banco do grupo). Fica na candidatura.
// O pagamento acontece fora do app: o banco só registra a confirmação de quem recebeu.
// NAO_APLICAVEL: pedido sem pagamento | PENDENTE: aguardando confirmação | CONFIRMADO: quem recebeu confirmou
public enum StatusPagamento {
    NAO_APLICAVEL, PENDENTE, CONFIRMADO
}

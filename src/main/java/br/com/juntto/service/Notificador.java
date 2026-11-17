package br.com.juntto.service;

import br.com.juntto.dao.RepositorioNotificacoes;
import br.com.juntto.model.Notificacao;
import br.com.juntto.model.TipoNotificacao;

// Usado pelos outros services para criar as notificações quando algo acontece.
// No modelo do banco toda notificação pertence a uma candidatura e a um pedido;
// por isso eventos sem candidatura (ex.: pedido expirado sem candidatos) não geram notificação.
public class Notificador {

    private RepositorioNotificacoes repositorio;

    public Notificador(RepositorioNotificacoes repositorio) {
        this.repositorio = repositorio;
    }

    public void enviar(String usuarioId, TipoNotificacao tipo, String mensagem, String idPedido, String idCandidatura) {
        if (usuarioId != null && idPedido != null && idCandidatura != null) {
            repositorio.adicionar(new Notificacao(usuarioId, tipo, mensagem, idPedido, idCandidatura));
        }
    }
}

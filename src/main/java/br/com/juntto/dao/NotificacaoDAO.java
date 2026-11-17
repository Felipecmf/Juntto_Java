package br.com.juntto.dao;

import br.com.juntto.model.Notificacao;
import br.com.juntto.model.TipoNotificacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

public class NotificacaoDAO implements RepositorioNotificacoes {

    private static final String COLUNAS = "id_notificacao, tb_can_ped_id_candidatura, tb_pedido_id_pedido, "
            + "tb_usuario_id_usuario, tipo_notificacao, mensagem, dt_criacao, lida";

    // [GS] DataSource recebido pelo construtor (configurado pelo Spring a partir das variáveis de ambiente)
    private DataSource dataSource;

    public NotificacaoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void adicionar(Notificacao n) {
        String sql = "INSERT INTO tb_notificacao (" + COLUNAS + ") VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection()) {
            n.setId(Jdbc.proximoId(c, "sq_tb_notificacao"));
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                Jdbc.preencher(ps, n.getId(), n.getIdCandidatura(), n.getIdPedido(), n.getIdUsuario(), n.getTipo(),
                        n.getMensagem(), n.getDtCriacao(), n.isLida());
                ps.execute();
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public Notificacao buscarPorId(String id) {
        ArrayList<Notificacao> lista = consultar("SELECT " + COLUNAS + " FROM tb_notificacao WHERE id_notificacao=?", id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public ArrayList<Notificacao> obterTodas() {
        return consultar("SELECT " + COLUNAS + " FROM tb_notificacao ORDER BY dt_criacao DESC");
    }

    // lida = null traz todas; lida = false traz só as não lidas (aviso e bolinha)
    @Override
    public ArrayList<Notificacao> obterPorUsuario(String usuarioId, Boolean lida) {
        if (lida == null) {
            return consultar("SELECT " + COLUNAS + " FROM tb_notificacao WHERE tb_usuario_id_usuario=? ORDER BY dt_criacao DESC",
                    usuarioId);
        }
        return consultar("SELECT " + COLUNAS + " FROM tb_notificacao WHERE tb_usuario_id_usuario=? AND lida=? "
                + "ORDER BY dt_criacao DESC", usuarioId, lida);
    }

    @Override
    public boolean atualizarLida(String id, boolean lida) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE tb_notificacao SET lida=? WHERE id_notificacao=?")) {
            ps.setString(1, lida ? "S" : "N");
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public boolean remover(String id) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tb_notificacao WHERE id_notificacao=?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private ArrayList<Notificacao> consultar(String sql, Object... parametros) {
        ArrayList<Notificacao> lista = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notificacao n = new Notificacao();
                    n.setId(rs.getString("id_notificacao"));
                    n.setIdCandidatura(rs.getString("tb_can_ped_id_candidatura"));
                    n.setIdPedido(rs.getString("tb_pedido_id_pedido"));
                    n.setIdUsuario(rs.getString("tb_usuario_id_usuario"));
                    n.setTipo(TipoNotificacao.valueOf(rs.getString("tipo_notificacao")));
                    n.setMensagem(rs.getString("mensagem"));
                    n.setDtCriacao(Jdbc.lerData(rs, "dt_criacao"));
                    n.setLida("S".equals(rs.getString("lida")));
                    lista.add(n);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }
}

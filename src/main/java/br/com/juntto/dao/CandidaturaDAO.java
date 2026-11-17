package br.com.juntto.dao;

import br.com.juntto.model.Candidatura;
import br.com.juntto.model.StatusCandidatura;
import br.com.juntto.model.StatusPagamento;
import br.com.juntto.model.StatusPedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import javax.sql.DataSource;

public class CandidaturaDAO implements RepositorioCandidaturas {

    // Candidatura + dados do prestador (nome, foto e avaliações, para o solicitante escolher) + dados do pedido
    private static final String SELECT =
            "SELECT ca.id_candidatura, ca.dt_oferta, ca.status, ca.status_pagamento, ca.dt_confirmacao_pagamento, "
          + "ca.tb_pedido_id_pedido, ca.tb_usuario_id_usuario, u.nome || ' ' || u.sobrenome AS nm_prestador, u.photo_url, "
          + "(SELECT ROUND(AVG(a.estrelas), 1) FROM " + Jdbc.AVALIACOES_RECEBIDAS + " a WHERE a.id_avaliado = u.id_usuario) AS media_estrelas, "
          + "(SELECT COUNT(*) FROM " + Jdbc.AVALIACOES_RECEBIDAS + " a WHERE a.id_avaliado = u.id_usuario) AS total_avaliacoes, "
          + "p.titulo_pedido, p.status AS status_pedido, p.tb_usuario_id_usuario AS id_solicitante "
          + "FROM tb_candidatura_pedido ca "
          + "INNER JOIN tb_usuario u ON u.id_usuario = ca.tb_usuario_id_usuario "
          + "INNER JOIN tb_pedido p ON p.id_pedido = ca.tb_pedido_id_pedido ";

    // [GS] DataSource recebido pelo construtor (configurado pelo Spring a partir das variáveis de ambiente)
    private DataSource dataSource;

    public CandidaturaDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void adicionar(Candidatura ca) {
        String sql = "INSERT INTO tb_candidatura_pedido (id_candidatura, tb_pedido_id_pedido, tb_usuario_id_usuario, "
                   + "dt_oferta, status, status_pagamento) VALUES (?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection()) {
            ca.setId(Jdbc.proximoId(c, "sq_tb_candidatura_pedido"));
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                Jdbc.preencher(ps, ca.getId(), ca.getIdPedido(), ca.getIdPrestador(), ca.getDtOferta(),
                        ca.getStatus(), ca.getStatusPagamento());
                ps.execute();
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public Candidatura buscarPorId(String id) {
        ArrayList<Candidatura> lista = consultar(SELECT + "WHERE ca.id_candidatura=?", id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public ArrayList<Candidatura> obterTodas() {
        return consultar(SELECT + "ORDER BY ca.dt_oferta DESC");
    }

    @Override
    public ArrayList<Candidatura> obterPorPedido(String idPedido) {
        return consultar(SELECT + "WHERE ca.tb_pedido_id_pedido=? ORDER BY ca.dt_oferta", idPedido);
    }

    @Override
    public ArrayList<Candidatura> obterPorUsuario(String usuarioId) {
        return consultar(SELECT + "WHERE ca.tb_usuario_id_usuario=? ORDER BY ca.dt_oferta DESC", usuarioId);
    }

    @Override
    public ArrayList<Candidatura> obterPendentes(String idPedido) {
        return consultar(SELECT + "WHERE ca.tb_pedido_id_pedido=? AND ca.status='PENDENTE'", idPedido);
    }

    @Override
    public Candidatura buscarEscolhida(String idPedido) {
        ArrayList<Candidatura> lista = consultar(SELECT + "WHERE ca.tb_pedido_id_pedido=? AND ca.status='ESCOLHIDO'", idPedido);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public boolean existe(String idPedido, String usuarioId) {
        String sql = "SELECT COUNT(*) FROM tb_candidatura_pedido WHERE tb_pedido_id_pedido=? AND tb_usuario_id_usuario=?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, idPedido);
            ps.setString(2, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public void alterarStatus(String id, StatusCandidatura status) {
        executar("UPDATE tb_candidatura_pedido SET status=? WHERE id_candidatura=?", status, id);
    }

    @Override
    public void alterarStatusPagamento(String id, StatusPagamento status, LocalDateTime dtConfirmacao) {
        executar("UPDATE tb_candidatura_pedido SET status_pagamento=?, dt_confirmacao_pagamento=? WHERE id_candidatura=?",
                status, dtConfirmacao, id);
    }

    private ArrayList<Candidatura> consultar(String sql, Object... parametros) {
        ArrayList<Candidatura> lista = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montar(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private void executar(String sql, Object... parametros) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            ps.execute();
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private Candidatura montar(ResultSet rs) throws SQLException {
        Candidatura ca = new Candidatura();
        ca.setId(rs.getString("id_candidatura"));
        ca.setDtOferta(Jdbc.lerData(rs, "dt_oferta"));
        ca.setStatus(StatusCandidatura.valueOf(rs.getString("status")));
        ca.setStatusPagamento(StatusPagamento.valueOf(rs.getString("status_pagamento")));
        ca.setDtConfirmacaoPagamento(Jdbc.lerData(rs, "dt_confirmacao_pagamento"));
        ca.setIdPedido(rs.getString("tb_pedido_id_pedido"));
        ca.setIdPrestador(rs.getString("tb_usuario_id_usuario"));
        ca.setNomePrestador(rs.getString("nm_prestador"));
        ca.setFotoPrestador(rs.getString("photo_url"));
        ca.setMediaEstrelasPrestador(Jdbc.lerDouble(rs, "media_estrelas"));
        ca.setTotalAvaliacoesPrestador(rs.getInt("total_avaliacoes"));
        ca.setTituloPedido(rs.getString("titulo_pedido"));
        ca.setStatusPedido(StatusPedido.valueOf(rs.getString("status_pedido")));
        ca.setIdSolicitante(rs.getString("id_solicitante"));
        return ca;
    }
}

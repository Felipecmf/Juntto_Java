package br.com.juntto.dao;

import br.com.juntto.model.Pedido;
import br.com.juntto.model.StatusPagamento;
import br.com.juntto.model.StatusPedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import javax.sql.DataSource;

public class PedidoDAO implements RepositorioPedidos {

    // Pedido + categoria + solicitante + quantidade de candidaturas aceitas.
    // Em AJUDA também traz a candidatura ACEITA (prestador e status do pagamento): só existe uma.
    // Em OFERTA podem ser várias aceitas: cada uma é consultada em /pedidos/{id}/candidaturas.
    private static final String SELECT =
            "SELECT p.id_pedido, p.tb_categoria_id_categoria, p.tb_usuario_id_usuario, p.titulo_pedido, p.tipo_publicacao, "
          + "p.status, p.descricao, p.dt_inicio, p.hora_inicio, p.dt_final, p.hora_final, p.valor, p.photo_url, p.caixinha, "
          + "p.recompensa, p.nao_incluir_pagamento, p.latitude, p.longitude, p.dt_recompensa, "
          + "c.categoria, u.nome || ' ' || u.sobrenome AS nm_solicitante, u.email AS email_solicitante, "
          + "ca.id_candidatura, ca.status_pagamento, ca.dt_confirmacao_pagamento, "
          + "pr.id_usuario AS id_prestador, pr.nome || ' ' || pr.sobrenome AS nm_prestador, pr.email AS email_prestador, "
          + "(SELECT COUNT(*) FROM tb_candidatura_pedido z WHERE z.tb_pedido_id_pedido = p.id_pedido AND z.status = 'ACEITA') AS total_aceitas "
          + "FROM tb_pedido p "
          + "INNER JOIN tb_categoria c ON c.id_categoria = p.tb_categoria_id_categoria "
          + "INNER JOIN tb_usuario u ON u.id_usuario = p.tb_usuario_id_usuario "
          + "LEFT JOIN tb_candidatura_pedido ca ON ca.tb_pedido_id_pedido = p.id_pedido AND ca.status = 'ACEITA' "
          + "AND p.tipo_publicacao = 'AJUDA' "
          + "LEFT JOIN tb_usuario pr ON pr.id_usuario = ca.tb_usuario_id_usuario ";

    // [GS] DataSource recebido pelo construtor (configurado pelo Spring a partir das variáveis de ambiente)
    private DataSource dataSource;

    public PedidoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void adicionar(Pedido p) {
        String sql = "INSERT INTO tb_pedido (id_pedido, tb_categoria_id_categoria, tb_usuario_id_usuario, titulo_pedido, "
                   + "tipo_publicacao, status, descricao, dt_inicio, hora_inicio, dt_final, hora_final, valor, photo_url, "
                   + "recompensa, nao_incluir_pagamento, latitude, longitude) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection()) {
            p.setId(Jdbc.proximoId(c, "sq_tb_pedido"));
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                // dt_inicio/hora_inicio e dt_final/hora_final recebem a data e o horário completos
                Jdbc.preencher(ps, p.getId(), p.getIdCategoria(), p.getIdSolicitante(), p.getTitulo(), p.getTipoPublicacao(),
                        p.getStatus(), p.getDescricao(), p.getDtInicio(), p.getDtInicio(), p.getDtFinal(), p.getDtFinal(),
                        p.getValor(), p.getFotoUrl(), p.getRecompensa(), p.semPagamento(), p.getLatitude(), p.getLongitude());
                ps.execute();
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public boolean atualizar(Pedido p) {
        return executar("UPDATE tb_pedido SET tb_categoria_id_categoria=?, titulo_pedido=?, tipo_publicacao=?, descricao=?, "
                        + "dt_inicio=?, hora_inicio=?, dt_final=?, hora_final=?, valor=?, photo_url=?, recompensa=?, "
                        + "nao_incluir_pagamento=?, latitude=?, longitude=? WHERE id_pedido=?",
                p.getIdCategoria(), p.getTitulo(), p.getTipoPublicacao(), p.getDescricao(), p.getDtInicio(), p.getDtInicio(),
                p.getDtFinal(), p.getDtFinal(), p.getValor(), p.getFotoUrl(), p.getRecompensa(), p.semPagamento(),
                p.getLatitude(), p.getLongitude(), p.getId()) > 0;
    }

    @Override
    public Pedido buscarPorId(String id) {
        ArrayList<Pedido> lista = consultar(SELECT + "WHERE p.id_pedido=?", id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    // Lista de oportunidades: só pedidos Abertos. Só entram na consulta os filtros informados.
    @Override
    public ArrayList<Pedido> obterAbertos(String tipo, String idCategoria, Double valorMin, Double valorMax, LocalDate data) {
        String sql = SELECT + "WHERE p.status='ABERTO'";
        ArrayList<Object> parametros = new ArrayList<>();
        if (tipo != null) {
            sql += " AND p.tipo_publicacao=?";
            parametros.add(tipo);
        }
        if (idCategoria != null && !idCategoria.isBlank()) {
            sql += " AND p.tb_categoria_id_categoria=?";
            parametros.add(idCategoria);
        }
        if (valorMin != null) {
            sql += " AND p.valor >= ?";
            parametros.add(valorMin);
        }
        if (valorMax != null) {
            sql += " AND p.valor <= ?";
            parametros.add(valorMax);
        }
        if (data != null) {
            sql += " AND p.dt_inicio >= ? AND p.dt_inicio < ?";
            parametros.add(data.atStartOfDay());
            parametros.add(data.plusDays(1).atStartOfDay());
        }
        sql += " ORDER BY p.dt_inicio";
        return consultar(sql, parametros.toArray());
    }

    @Override
    public ArrayList<Pedido> obterPorSolicitante(String usuarioId) {
        return consultar(SELECT + "WHERE p.tb_usuario_id_usuario=? ORDER BY p.dt_inicio DESC", usuarioId);
    }

    // Pedidos em que o usuário se candidatou (aceito ou não, inclusive os que ele desistiu)
    @Override
    public ArrayList<Pedido> obterPorPrestador(String usuarioId) {
        return consultar(SELECT + "WHERE EXISTS (SELECT 1 FROM tb_candidatura_pedido x WHERE x.tb_pedido_id_pedido = p.id_pedido "
                + "AND x.tb_usuario_id_usuario=?) ORDER BY p.dt_inicio DESC", usuarioId);
    }

    // Abertos com a data vencida e sem ninguém aceito (uma OFERTA com aceitos fica aberta até o dono concluir)
    @Override
    public ArrayList<Pedido> obterAbertosVencidos(LocalDateTime agora) {
        return consultar(SELECT + "WHERE p.status='ABERTO' AND p.dt_inicio < ? AND NOT EXISTS (SELECT 1 FROM "
                + "tb_candidatura_pedido v WHERE v.tb_pedido_id_pedido = p.id_pedido AND v.status='ACEITA')", agora);
    }

    // Já recebeu candidaturas? Então o pedido é cancelado em vez de apagado.
    @Override
    public boolean possuiVinculos(String id) {
        return contar("SELECT COUNT(*) FROM tb_candidatura_pedido WHERE tb_pedido_id_pedido=?", id) > 0;
    }

    // Serviços que ainda não terminaram em que o usuário participa (como dono ou como candidato aceito):
    // AJUDA em andamento, OFERTA aberta com aceitos, ou pagamento ainda pendente de confirmação
    @Override
    public int contarAtivosDoUsuario(String usuarioId) {
        return contar("SELECT COUNT(*) FROM tb_pedido p JOIN tb_candidatura_pedido a ON a.tb_pedido_id_pedido = p.id_pedido "
                + "WHERE a.status='ACEITA' AND (p.tb_usuario_id_usuario=? OR a.tb_usuario_id_usuario=?) "
                + "AND (p.status IN ('ABERTO','EM_ANDAMENTO') OR (p.status='CONCLUIDO' AND a.status_pagamento='PENDENTE'))",
                usuarioId, usuarioId);
    }

    @Override
    public boolean remover(String id) {
        return executar("DELETE FROM tb_pedido WHERE id_pedido=?", id) > 0;
    }

    @Override
    public boolean marcarEmAndamento(String id) {
        return executar("UPDATE tb_pedido SET status='EM_ANDAMENTO' WHERE id_pedido=? AND status='ABERTO'", id) > 0;
    }

    @Override
    public void alterarStatus(String id, StatusPedido status) {
        executar("UPDATE tb_pedido SET status=? WHERE id_pedido=?", status, id);
    }

    @Override
    public void concluir(String id, LocalDateTime quando, Double caixinha) {
        executar("UPDATE tb_pedido SET status='CONCLUIDO', dt_final=?, hora_final=?, caixinha=? WHERE id_pedido=?",
                quando, quando, caixinha, id);
    }

    @Override
    public void confirmarRecompensa(String id, LocalDateTime quando) {
        executar("UPDATE tb_pedido SET dt_recompensa=? WHERE id_pedido=?", quando, id);
    }

    // ------------------------------ auxiliares ------------------------------

    private ArrayList<Pedido> consultar(String sql, Object... parametros) {
        ArrayList<Pedido> pedidos = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(montar(rs));
                }
            }
            return pedidos;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private int executar(String sql, Object... parametros) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private int contar(String sql, Object... parametros) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private Pedido montar(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getString("id_pedido"));
        p.setIdCategoria(rs.getString("tb_categoria_id_categoria"));
        p.setNomeCategoria(rs.getString("categoria"));
        p.setIdSolicitante(rs.getString("tb_usuario_id_usuario"));
        p.setNomeSolicitante(rs.getString("nm_solicitante"));
        p.setEmailSolicitante(rs.getString("email_solicitante"));
        p.setTitulo(rs.getString("titulo_pedido"));
        p.setTipoPublicacao(rs.getString("tipo_publicacao"));
        p.setStatus(StatusPedido.valueOf(rs.getString("status")));
        p.setDescricao(rs.getString("descricao"));
        p.setDtInicio(Jdbc.lerDataHora(rs, "dt_inicio", "hora_inicio"));
        p.setDtFinal(Jdbc.lerDataHora(rs, "dt_final", "hora_final"));
        p.setValor(Jdbc.lerDouble(rs, "valor"));
        p.setFotoUrl(rs.getString("photo_url"));
        p.setCaixinha(Jdbc.lerDouble(rs, "caixinha"));
        p.setRecompensa(rs.getString("recompensa"));
        p.setNaoIncluirPagamento("S".equals(rs.getString("nao_incluir_pagamento")));
        p.setLatitude(Jdbc.lerDouble(rs, "latitude"));
        p.setLongitude(Jdbc.lerDouble(rs, "longitude"));
        p.setDtRecompensa(Jdbc.lerData(rs, "dt_recompensa"));
        p.setTotalAceitas(rs.getInt("total_aceitas"));
        p.setIdCandidaturaAceita(rs.getString("id_candidatura"));
        String statusPagamento = rs.getString("status_pagamento");
        p.setStatusPagamento(statusPagamento == null ? null : StatusPagamento.valueOf(statusPagamento));
        p.setDtConfirmacaoPagamento(Jdbc.lerData(rs, "dt_confirmacao_pagamento"));
        p.setIdPrestador(rs.getString("id_prestador"));
        p.setNomePrestador(rs.getString("nm_prestador"));
        p.setEmailPrestador(rs.getString("email_prestador"));
        return p;
    }
}

package br.com.juntto.dao;

import br.com.juntto.model.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

public class FeedbackDAO implements RepositorioFeedbacks {

    // Feedback + pedido + quem ajudou + quem recebeu a ajuda.
    // Em AJUDA quem ajuda é o candidato aceito; em OFERTA quem ajuda é o dono da publicação.
    private static final String SELECT =
            "SELECT * FROM (SELECT f.id_feedback, f.tb_can_ped_id_candidatura, f.estrelas_usu_ajudante, f.estrelas_usu_pedido, "
          + "f.feedback_usu_ajudante, f.feedback_usu_pedido, f.dt_avaliacao_usu_ajudante, f.dt_avaliacao_usu_pedido, "
          + "p.id_pedido, p.titulo_pedido, "
          + "CASE WHEN p.tipo_publicacao = 'OFERTA' THEN dono.id_usuario ELSE cand.id_usuario END AS id_ajudante, "
          + "CASE WHEN p.tipo_publicacao = 'OFERTA' THEN dono.nome || ' ' || dono.sobrenome "
          + "ELSE cand.nome || ' ' || cand.sobrenome END AS nm_ajudante, "
          + "CASE WHEN p.tipo_publicacao = 'OFERTA' THEN cand.id_usuario ELSE dono.id_usuario END AS id_ajudado, "
          + "CASE WHEN p.tipo_publicacao = 'OFERTA' THEN cand.nome || ' ' || cand.sobrenome "
          + "ELSE dono.nome || ' ' || dono.sobrenome END AS nm_ajudado "
          + "FROM tb_feedbacks f "
          + "INNER JOIN tb_candidatura_pedido ca ON ca.id_candidatura = f.tb_can_ped_id_candidatura "
          + "INNER JOIN tb_pedido p ON p.id_pedido = ca.tb_pedido_id_pedido "
          + "INNER JOIN tb_usuario cand ON cand.id_usuario = ca.tb_usuario_id_usuario "
          + "INNER JOIN tb_usuario dono ON dono.id_usuario = p.tb_usuario_id_usuario) x ";

    // [GS] DataSource recebido pelo construtor (configurado pelo Spring a partir das variáveis de ambiente)
    private DataSource dataSource;

    public FeedbackDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void adicionar(Feedback f) {
        String sql = "INSERT INTO tb_feedbacks (id_feedback, tb_can_ped_id_candidatura, estrelas_usu_ajudante, "
                   + "estrelas_usu_pedido, feedback_usu_ajudante, feedback_usu_pedido, dt_avaliacao_usu_ajudante, "
                   + "dt_avaliacao_usu_pedido) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection()) {
            f.setId(Jdbc.proximoId(c, "sq_tb_feedbacks"));
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                Jdbc.preencher(ps, f.getId(), f.getIdCandidatura(), texto(f.getEstrelasAjudante()),
                        texto(f.getEstrelasAjudado()), f.getFeedbackAjudante(), f.getFeedbackAjudado(),
                        f.getDtAvaliacaoAjudante(), f.getDtAvaliacaoAjudado());
                ps.execute();
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public Feedback buscarPorId(String id) {
        ArrayList<Feedback> lista = consultar(SELECT + "WHERE x.id_feedback=?", id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public Feedback buscarPorCandidatura(String idCandidatura) {
        ArrayList<Feedback> lista = consultar(SELECT + "WHERE x.tb_can_ped_id_candidatura=?", idCandidatura);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public ArrayList<Feedback> obterTodos() {
        return consultar(SELECT + "ORDER BY x.id_feedback");
    }

    @Override
    public ArrayList<Feedback> obterPorPedido(String idPedido) {
        return consultar(SELECT + "WHERE x.id_pedido=?", idPedido);
    }

    // Avaliações que o usuário RECEBEU: como ajudante (estrelas_usu_ajudante) ou como quem recebeu a ajuda (estrelas_usu_pedido)
    @Override
    public ArrayList<Feedback> obterRecebidos(String usuarioId) {
        return consultar(SELECT + "WHERE (x.id_ajudante=? AND x.estrelas_usu_ajudante IS NOT NULL) "
                + "OR (x.id_ajudado=? AND x.estrelas_usu_pedido IS NOT NULL) ORDER BY x.id_feedback DESC", usuarioId, usuarioId);
    }

    @Override
    public boolean atualizar(Feedback f) {
        String sql = "UPDATE tb_feedbacks SET estrelas_usu_ajudante=?, estrelas_usu_pedido=?, feedback_usu_ajudante=?, "
                   + "feedback_usu_pedido=?, dt_avaliacao_usu_ajudante=?, dt_avaliacao_usu_pedido=? WHERE id_feedback=?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, texto(f.getEstrelasAjudante()), texto(f.getEstrelasAjudado()), f.getFeedbackAjudante(),
                    f.getFeedbackAjudado(), f.getDtAvaliacaoAjudante(), f.getDtAvaliacaoAjudado(), f.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public boolean remover(String id) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tb_feedbacks WHERE id_feedback=?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    // estrelas ficam em CHAR(1): 4 -> "4"
    private static String texto(Integer estrelas) {
        return estrelas == null ? null : String.valueOf(estrelas);
    }

    private ArrayList<Feedback> consultar(String sql, Object... parametros) {
        ArrayList<Feedback> lista = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Feedback f = new Feedback();
                    f.setId(rs.getString("id_feedback"));
                    f.setIdCandidatura(rs.getString("tb_can_ped_id_candidatura"));
                    f.setEstrelasAjudante(Jdbc.lerInteiro(rs, "estrelas_usu_ajudante"));
                    f.setEstrelasAjudado(Jdbc.lerInteiro(rs, "estrelas_usu_pedido"));
                    f.setFeedbackAjudante(rs.getString("feedback_usu_ajudante"));
                    f.setFeedbackAjudado(rs.getString("feedback_usu_pedido"));
                    f.setDtAvaliacaoAjudante(Jdbc.lerData(rs, "dt_avaliacao_usu_ajudante"));
                    f.setDtAvaliacaoAjudado(Jdbc.lerData(rs, "dt_avaliacao_usu_pedido"));
                    f.setIdPedido(rs.getString("id_pedido"));
                    f.setTituloPedido(rs.getString("titulo_pedido"));
                    f.setIdAjudante(rs.getString("id_ajudante"));
                    f.setNomeAjudante(rs.getString("nm_ajudante"));
                    f.setIdAjudado(rs.getString("id_ajudado"));
                    f.setNomeAjudado(rs.getString("nm_ajudado"));
                    lista.add(f);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }
}

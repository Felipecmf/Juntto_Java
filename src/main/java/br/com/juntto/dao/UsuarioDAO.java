package br.com.juntto.dao;

import br.com.juntto.model.Categoria;
import br.com.juntto.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

public class UsuarioDAO implements RepositorioUsuarios {

    // Usuário + média de estrelas e total de avaliações recebidas
    private static final String SELECT =
            "SELECT u.id_usuario, u.email, u.senha, u.nome, u.sobrenome, u.photo_url, u.latitude, u.longitude, "
          + "(SELECT ROUND(AVG(a.estrelas), 1) FROM " + Jdbc.AVALIACOES_RECEBIDAS + " a WHERE a.id_avaliado = u.id_usuario) AS media_estrelas, "
          + "(SELECT COUNT(*) FROM " + Jdbc.AVALIACOES_RECEBIDAS + " a WHERE a.id_avaliado = u.id_usuario) AS total_avaliacoes "
          + "FROM tb_usuario u ";

    // [GS] O DataSource (conexão configurada pelo Spring) é recebido pelo construtor
    private DataSource dataSource;

    public UsuarioDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void adicionar(Usuario u) {
        String sql = "INSERT INTO tb_usuario (id_usuario, email, senha, nome, sobrenome, photo_url, latitude, longitude) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection()) {
            u.setId(Jdbc.proximoId(c, "sq_tb_usuario"));
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, u.getId());
                ps.setString(2, u.getEmail());
                ps.setString(3, u.getSenha());
                ps.setString(4, u.getNome());
                ps.setString(5, u.getSobrenome());
                ps.setString(6, u.getFotoUrl());
                Jdbc.setDouble(ps, 7, u.getLatitude());
                Jdbc.setDouble(ps, 8, u.getLongitude());
                ps.execute();
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public boolean atualizar(Usuario u) {
        return executar("UPDATE tb_usuario SET nome=?, sobrenome=?, photo_url=?, latitude=?, longitude=? WHERE id_usuario=?",
                u.getNome(), u.getSobrenome(), u.getFotoUrl(), u.getLatitude(), u.getLongitude(), u.getId()) > 0;
    }

    @Override
    public Usuario buscarPorId(String id) {
        ArrayList<Usuario> lista = consultar(SELECT + "WHERE u.id_usuario=?", id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        ArrayList<Usuario> lista = consultar(SELECT + "WHERE u.email=?", email.toLowerCase());
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public ArrayList<Usuario> obterTodos() {
        return consultar(SELECT + "WHERE u.email NOT LIKE 'removido-%' ORDER BY u.nome");
    }

    // Já publicou pedido ou se candidatou? Então a conta é anonimizada em vez de apagada
    @Override
    public boolean possuiVinculos(String id) {
        String sql = "SELECT (SELECT COUNT(*) FROM tb_pedido WHERE tb_usuario_id_usuario=?) "
                   + "+ (SELECT COUNT(*) FROM tb_candidatura_pedido WHERE tb_usuario_id_usuario=?) FROM DUAL";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public void remover(String id) {
        executar("DELETE FROM tb_usuario_categoria WHERE tb_usuario_id_usuario=?", id);
        executar("DELETE FROM tb_usuario WHERE id_usuario=?", id);
    }

    // Exclusão com histórico: apaga os dados pessoais e mantém o registro para não quebrar pedidos e avaliações
    @Override
    public void anonimizar(String id) {
        executar("DELETE FROM tb_usuario_categoria WHERE tb_usuario_id_usuario=?", id);
        executar("UPDATE tb_usuario SET email=?, senha='-', nome='Usuário', sobrenome='removido', photo_url=NULL, "
                + "latitude=NULL, longitude=NULL WHERE id_usuario=?", "removido-" + id + "@juntto.com", id);
    }

    @Override
    public ArrayList<Categoria> obterTags(String id) {
        String sql = "SELECT c.id_categoria, c.categoria FROM tb_categoria c "
                   + "INNER JOIN tb_usuario_categoria uc ON uc.tb_categoria_id_categoria = c.id_categoria "
                   + "WHERE uc.tb_usuario_id_usuario=? ORDER BY c.categoria";
        ArrayList<Categoria> tags = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(new Categoria(rs.getString("id_categoria"), rs.getString("categoria")));
                }
            }
            return tags;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    // Substitui o conjunto inteiro: apaga as tags antigas e grava as novas
    @Override
    public void salvarTags(String id, ArrayList<String> idsCategorias) {
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM tb_usuario_categoria WHERE tb_usuario_id_usuario=?")) {
                ps.setString(1, id);
                ps.execute();
            }
            for (String idCategoria : idsCategorias) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO tb_usuario_categoria (tb_categoria_id_categoria, tb_usuario_id_usuario) VALUES (?,?)")) {
                    ps.setString(1, idCategoria);
                    ps.setString(2, id);
                    ps.execute();
                }
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private ArrayList<Usuario> consultar(String sql, Object... parametros) {
        ArrayList<Usuario> lista = new ArrayList<>();
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

    private int executar(String sql, Object... parametros) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private Usuario montar(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getString("id_usuario"));
        u.setEmail(rs.getString("email"));
        u.setSenha(rs.getString("senha"));
        u.setNome(rs.getString("nome"));
        u.setSobrenome(rs.getString("sobrenome"));
        u.setFotoUrl(rs.getString("photo_url"));
        u.setLatitude(Jdbc.lerDouble(rs, "latitude"));
        u.setLongitude(Jdbc.lerDouble(rs, "longitude"));
        u.setMediaEstrelas(Jdbc.lerDouble(rs, "media_estrelas"));
        u.setTotalAvaliacoes(rs.getInt("total_avaliacoes"));
        return u;
    }
}

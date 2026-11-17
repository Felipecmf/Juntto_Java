package br.com.juntto.dao;

import br.com.juntto.model.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

public class CategoriaDAO implements RepositorioCategorias {

    // [GS] DataSource recebido pelo construtor (configurado pelo Spring a partir das variáveis de ambiente)
    private DataSource dataSource;

    public CategoriaDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void adicionar(Categoria categoria) {
        try (Connection c = dataSource.getConnection()) {
            categoria.setId(Jdbc.proximoId(c, "sq_tb_categoria"));
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO tb_categoria (id_categoria, categoria) VALUES (?,?)")) {
                ps.setString(1, categoria.getId());
                ps.setString(2, categoria.getNome());
                ps.execute();
            }
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public boolean atualizar(Categoria categoria) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE tb_categoria SET categoria=? WHERE id_categoria=?")) {
            ps.setString(1, categoria.getNome());
            ps.setString(2, categoria.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    @Override
    public Categoria buscarPorId(String id) {
        ArrayList<Categoria> lista = consultar("SELECT id_categoria, categoria FROM tb_categoria WHERE id_categoria=?", id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public ArrayList<Categoria> obterTodas() {
        return consultar("SELECT id_categoria, categoria FROM tb_categoria ORDER BY categoria");
    }

    // Está em uso por algum pedido ou tag? Então não pode ser removida (409)
    @Override
    public boolean emUso(String id) {
        String sql = "SELECT (SELECT COUNT(*) FROM tb_pedido WHERE tb_categoria_id_categoria=?) "
                   + "+ (SELECT COUNT(*) FROM tb_usuario_categoria WHERE tb_categoria_id_categoria=?) FROM DUAL";
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
    public boolean remover(String id) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tb_categoria WHERE id_categoria=?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }

    private ArrayList<Categoria> consultar(String sql, Object... parametros) {
        ArrayList<Categoria> lista = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            Jdbc.preencher(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Categoria(rs.getString("id_categoria"), rs.getString("categoria")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw ErroSql.traduzir(e);
        }
    }
}

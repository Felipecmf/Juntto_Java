package br.com.juntto.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

// Funções de apoio usadas por todos os DAOs (valores nulos, datas e sequences)
class Jdbc {

    // Avaliações RECEBIDAS por cada usuário, para calcular a média de estrelas.
    // estrelas_usu_ajudante = nota SOBRE quem ajudou | estrelas_usu_pedido = nota SOBRE quem recebeu a ajuda.
    // Em AJUDA quem ajuda é o candidato; em OFERTA quem ajuda é o dono da publicação.
    // Usada como subconsulta: ...FROM AVALIACOES_RECEBIDAS a WHERE a.id_avaliado = <coluna do usuário>
    static final String AVALIACOES_RECEBIDAS =
            "(SELECT TO_NUMBER(f.estrelas_usu_ajudante) AS estrelas, "
          + "CASE WHEN p.tipo_publicacao = 'OFERTA' THEN p.tb_usuario_id_usuario ELSE c.tb_usuario_id_usuario END AS id_avaliado "
          + "FROM tb_feedbacks f JOIN tb_candidatura_pedido c ON c.id_candidatura = f.tb_can_ped_id_candidatura "
          + "JOIN tb_pedido p ON p.id_pedido = c.tb_pedido_id_pedido "
          + "WHERE f.estrelas_usu_ajudante IS NOT NULL "
          + "UNION ALL "
          + "SELECT TO_NUMBER(f.estrelas_usu_pedido), "
          + "CASE WHEN p.tipo_publicacao = 'OFERTA' THEN c.tb_usuario_id_usuario ELSE p.tb_usuario_id_usuario END "
          + "FROM tb_feedbacks f JOIN tb_candidatura_pedido c ON c.id_candidatura = f.tb_can_ped_id_candidatura "
          + "JOIN tb_pedido p ON p.id_pedido = c.tb_pedido_id_pedido "
          + "WHERE f.estrelas_usu_pedido IS NOT NULL)";

    // Busca o próximo número da sequence (ex.: sq_tb_pedido) para usar como chave primária.
    // As chaves do modelo são VARCHAR2, então o número é gravado como texto ("1", "2"...).
    static String proximoId(Connection c, String sequence) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT TO_CHAR(" + sequence + ".NEXTVAL) FROM DUAL");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    // Junta a data de uma coluna DATE com o horário de outra (ex.: dt_inicio + hora_inicio)
    static LocalDateTime lerDataHora(ResultSet rs, String colunaData, String colunaHora) throws SQLException {
        LocalDateTime data = lerData(rs, colunaData);
        LocalDateTime hora = lerData(rs, colunaHora);
        if (data == null) {
            return hora;
        }
        return hora == null ? data : data.toLocalDate().atTime(hora.toLocalTime());
    }

    static Integer lerInteiro(ResultSet rs, String coluna) throws SQLException {
        String v = rs.getString(coluna);
        return v == null || v.isBlank() ? null : Integer.valueOf(v.trim());
    }

    static void setLong(PreparedStatement ps, int posicao, Long valor) throws SQLException {
        if (valor == null) {
            ps.setNull(posicao, Types.NUMERIC);
        } else {
            ps.setLong(posicao, valor);
        }
    }

    static void setDouble(PreparedStatement ps, int posicao, Double valor) throws SQLException {
        if (valor == null) {
            ps.setNull(posicao, Types.NUMERIC);
        } else {
            ps.setDouble(posicao, valor);
        }
    }

    static void setData(PreparedStatement ps, int posicao, LocalDateTime valor) throws SQLException {
        if (valor == null) {
            ps.setNull(posicao, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(posicao, Timestamp.valueOf(valor));
        }
    }

    static Long lerLong(ResultSet rs, String coluna) throws SQLException {
        long v = rs.getLong(coluna);
        return rs.wasNull() ? null : v;
    }

    static Double lerDouble(ResultSet rs, String coluna) throws SQLException {
        double v = rs.getDouble(coluna);
        return rs.wasNull() ? null : v;
    }

    static LocalDateTime lerData(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toLocalDateTime();
    }

    // Preenche os "?" na ordem: aceita String, Long, Double, Boolean (S/N), Enum e LocalDateTime
    static void preencher(PreparedStatement ps, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            Object v = parametros[i];
            int pos = i + 1;
            if (v == null) {
                ps.setNull(pos, Types.VARCHAR);
            } else if (v instanceof Long) {
                ps.setLong(pos, (Long) v);
            } else if (v instanceof Double) {
                ps.setDouble(pos, (Double) v);
            } else if (v instanceof Boolean) {
                ps.setString(pos, (Boolean) v ? "S" : "N");
            } else if (v instanceof Enum) {
                ps.setString(pos, ((Enum<?>) v).name());
            } else if (v instanceof LocalDateTime) {
                ps.setTimestamp(pos, Timestamp.valueOf((LocalDateTime) v));
            } else {
                ps.setString(pos, v.toString());
            }
        }
    }
}

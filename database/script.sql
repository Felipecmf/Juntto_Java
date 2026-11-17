-- =====================================================================
-- Junttô - script do banco usado pela API Java
-- Tabelas e colunas IGUAIS ao modelo oficial do grupo (Data Modeler).
-- Acrescentado pela API: as sequences sq_tb_* (geram os IDs) e dados de teste.
-- Pode ser executado de novo: apaga e recria tudo. Execute com F5 no SQL Developer.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Limpeza (tabelas da API antiga, da versão anterior e do UrbanLink, se existirem)
-- ---------------------------------------------------------------------
BEGIN
    FOR t IN (SELECT table_name FROM user_tables WHERE table_name IN (
        'TB_FEEDBACKS','TB_NOTIFICACAO','TB_CANDIDATURA_PEDIDO','TB_PEDIDO','TB_USUARIO_CATEGORIA',
        'TB_CATEGORIA','TB_USUARIO',
        'TB_DENUNCIA','TB_COMENTARIO','TB_CANDIDATURA','TB_ACEITE','TB_PEDIDO_FOTO',
        'IMPACTO_AMBIENTAL','TRANSPORTE','VOUCHER','USUARIO')) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
    END LOOP;
    FOR s IN (SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE 'SQ_TB_%') LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
    END LOOP;
END;
/

-- ---------------------------------------------------------------------
-- Tabelas (modelo oficial)
-- ---------------------------------------------------------------------
CREATE TABLE tb_usuario (
    id_usuario  VARCHAR2(100) NOT NULL,
    email       VARCHAR2(100) NOT NULL,
    senha       VARCHAR2(255) NOT NULL,
    nome        VARCHAR2(20),
    sobrenome   VARCHAR2(20),
    photo_url   VARCHAR2(200),
    latitude    NUMBER(10, 8),
    longitude   NUMBER(11, 8),
    CONSTRAINT tb_usuario_pk PRIMARY KEY (id_usuario),
    CONSTRAINT tb_usuario_email_uc UNIQUE (email)
);
COMMENT ON TABLE tb_usuario IS 'Pessoas cadastradas: a mesma conta pede e oferece ajuda';

CREATE TABLE tb_categoria (
    id_categoria VARCHAR2(100) NOT NULL,
    categoria    VARCHAR2(100) NOT NULL,
    CONSTRAINT tb_categoria_pk PRIMARY KEY (id_categoria),
    CONSTRAINT tb_categoria_categoria_uc UNIQUE (categoria)
);
COMMENT ON TABLE tb_categoria IS 'Tipos de ajuda (montagem, reparos, limpeza...)';

CREATE TABLE tb_usuario_categoria (
    tb_categoria_id_categoria VARCHAR2(100) NOT NULL,
    tb_usuario_id_usuario     VARCHAR2(100) NOT NULL,
    CONSTRAINT tb_usuario_categoria_pk PRIMARY KEY (tb_categoria_id_categoria, tb_usuario_id_usuario),
    CONSTRAINT tb_usr_cat_tb_cat_fk FOREIGN KEY (tb_categoria_id_categoria) REFERENCES tb_categoria (id_categoria),
    CONSTRAINT tb_usr_cat_tb_usr_fk FOREIGN KEY (tb_usuario_id_usuario) REFERENCES tb_usuario (id_usuario)
);
COMMENT ON TABLE tb_usuario_categoria IS 'Tags: categorias em que cada usuário ajuda (N:N)';

CREATE TABLE tb_pedido (
    id_pedido                 VARCHAR2(100) NOT NULL,
    tb_categoria_id_categoria VARCHAR2(100) NOT NULL,
    tb_usuario_id_usuario     VARCHAR2(100) NOT NULL,
    titulo_pedido             VARCHAR2(50)  NOT NULL,
    tipo_publicacao           VARCHAR2(50)  NOT NULL,
    status                    VARCHAR2(100) NOT NULL,
    descricao                 VARCHAR2(500),
    dt_inicio                 DATE,
    valor                     NUMBER(10, 2),
    photo_url                 VARCHAR2(200),
    caixinha                  NUMBER(10, 2),
    recompensa                VARCHAR2(300),
    nao_incluir_pagamento     CHAR(1),
    hora_inicio               DATE,
    dt_final                  DATE,
    hora_final                DATE,
    latitude                  NUMBER(10, 8),
    longitude                 NUMBER(11, 8),
    dt_recompensa             DATE,
    CONSTRAINT tb_pedido_pk PRIMARY KEY (id_pedido),
    CONSTRAINT tb_ped_tb_cat_fk FOREIGN KEY (tb_categoria_id_categoria) REFERENCES tb_categoria (id_categoria),
    CONSTRAINT tb_ped_tb_usr_fk FOREIGN KEY (tb_usuario_id_usuario) REFERENCES tb_usuario (id_usuario)
);
COMMENT ON TABLE tb_pedido IS 'Pedidos de ajuda publicados (1:N com usuário e categoria)';

CREATE TABLE tb_candidatura_pedido (
    id_candidatura           VARCHAR2(100) NOT NULL,
    tb_pedido_id_pedido      VARCHAR2(100) NOT NULL,
    tb_usuario_id_usuario    VARCHAR2(100) NOT NULL,
    dt_oferta                DATE          NOT NULL,
    status                   VARCHAR2(20)  NOT NULL,
    status_pagamento         VARCHAR2(20)  NOT NULL,
    dt_confirmacao_pagamento DATE,
    CONSTRAINT tb_candidatura_pedido_pk PRIMARY KEY (id_candidatura),
    CONSTRAINT tb_can_ped_tb_ped_fk FOREIGN KEY (tb_pedido_id_pedido) REFERENCES tb_pedido (id_pedido),
    CONSTRAINT tb_can_ped_tb_usr_fk FOREIGN KEY (tb_usuario_id_usuario) REFERENCES tb_usuario (id_usuario)
);
COMMENT ON TABLE tb_candidatura_pedido IS 'Candidaturas de prestadores aos pedidos (1:N com pedido)';

CREATE TABLE tb_notificacao (
    id_notificacao            VARCHAR2(100) NOT NULL,
    tb_can_ped_id_candidatura VARCHAR2(100) NOT NULL,
    tb_pedido_id_pedido       VARCHAR2(100) NOT NULL,
    tb_usuario_id_usuario     VARCHAR2(100) NOT NULL,
    tipo_notificacao          VARCHAR2(30)  NOT NULL,
    mensagem                  VARCHAR2(500) NOT NULL,
    dt_criacao                DATE          NOT NULL,
    lida                      CHAR(1)       NOT NULL,
    CONSTRAINT tb_notificacao_pk PRIMARY KEY (id_notificacao),
    CONSTRAINT tb_not_tb_can_ped_fk FOREIGN KEY (tb_can_ped_id_candidatura) REFERENCES tb_candidatura_pedido (id_candidatura),
    CONSTRAINT tb_not_tb_ped_fk FOREIGN KEY (tb_pedido_id_pedido) REFERENCES tb_pedido (id_pedido),
    CONSTRAINT tb_not_tb_usr_fk FOREIGN KEY (tb_usuario_id_usuario) REFERENCES tb_usuario (id_usuario)
);
COMMENT ON TABLE tb_notificacao IS 'Avisos para os usuários sobre candidaturas e pedidos';

CREATE TABLE tb_feedbacks (
    id_feedback               VARCHAR2(100) NOT NULL,
    tb_can_ped_id_candidatura VARCHAR2(100) NOT NULL,
    estrelas_usu_ajudante     CHAR(1),
    estrelas_usu_pedido       CHAR(1),
    feedback_usu_ajudante     VARCHAR2(500),
    feedback_usu_pedido       VARCHAR2(500),
    dt_avaliacao_usu_ajudante DATE,
    dt_avaliacao_usu_pedido   DATE,
    CONSTRAINT tb_feedbacks_pk PRIMARY KEY (id_feedback),
    CONSTRAINT tb_fee_id_can_uc UNIQUE (tb_can_ped_id_candidatura),
    CONSTRAINT tb_fee_tb_can_ped_fk FOREIGN KEY (tb_can_ped_id_candidatura) REFERENCES tb_candidatura_pedido (id_candidatura)
);
COMMENT ON TABLE tb_feedbacks IS 'Avaliação do serviço: o solicitante avalia o ajudante e o ajudante avalia o solicitante';

-- ---------------------------------------------------------------------
-- Sequences: geram os IDs (gravados como texto, porque as chaves do modelo são VARCHAR2)
-- ---------------------------------------------------------------------
CREATE SEQUENCE sq_tb_usuario START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_categoria START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_pedido START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_candidatura_pedido START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_notificacao START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE sq_tb_feedbacks START WITH 1 INCREMENT BY 1 NOCACHE;

-- ---------------------------------------------------------------------
-- Dados de teste
-- ---------------------------------------------------------------------
INSERT INTO tb_categoria VALUES (TO_CHAR(sq_tb_categoria.NEXTVAL), 'Montagem de móveis');
INSERT INTO tb_categoria VALUES (TO_CHAR(sq_tb_categoria.NEXTVAL), 'Pequenos reparos');
INSERT INTO tb_categoria VALUES (TO_CHAR(sq_tb_categoria.NEXTVAL), 'Limpeza');
INSERT INTO tb_categoria VALUES (TO_CHAR(sq_tb_categoria.NEXTVAL), 'Jardinagem');
INSERT INTO tb_categoria VALUES (TO_CHAR(sq_tb_categoria.NEXTVAL), 'Compras e entregas');
INSERT INTO tb_categoria VALUES (TO_CHAR(sq_tb_categoria.NEXTVAL), 'Aulas e reforço escolar');

-- Senha de todos: senha123 (SHA-256)
INSERT INTO tb_usuario VALUES (TO_CHAR(sq_tb_usuario.NEXTVAL), 'ana@email.com',    '55a5e9e78207b4df8699d60886fa070079463547b095d1a05bc719bb4e6cd251', 'Ana',    'Souza',   NULL, -23.56120000, -46.65580000);
INSERT INTO tb_usuario VALUES (TO_CHAR(sq_tb_usuario.NEXTVAL), 'bruno@email.com',  '55a5e9e78207b4df8699d60886fa070079463547b095d1a05bc719bb4e6cd251', 'Bruno',  'Lima',    NULL, -23.56700000, -46.68400000);
INSERT INTO tb_usuario VALUES (TO_CHAR(sq_tb_usuario.NEXTVAL), 'carla@email.com',  '55a5e9e78207b4df8699d60886fa070079463547b095d1a05bc719bb4e6cd251', 'Carla',  'Mendes',  NULL, -23.58900000, -46.63400000);
INSERT INTO tb_usuario VALUES (TO_CHAR(sq_tb_usuario.NEXTVAL), 'diego@email.com',  '55a5e9e78207b4df8699d60886fa070079463547b095d1a05bc719bb4e6cd251', 'Diego',  'Almeida', NULL, -23.58600000, -46.63800000);
INSERT INTO tb_usuario VALUES (TO_CHAR(sq_tb_usuario.NEXTVAL), 'elisa@email.com',  '55a5e9e78207b4df8699d60886fa070079463547b095d1a05bc719bb4e6cd251', 'Elisa',  'Rocha',   NULL, -23.55300000, -46.69100000);
INSERT INTO tb_usuario VALUES (TO_CHAR(sq_tb_usuario.NEXTVAL), 'felipe@email.com', '55a5e9e78207b4df8699d60886fa070079463547b095d1a05bc719bb4e6cd251', 'Felipe', 'Costa',   NULL, -23.56100000, -46.65600000);

-- Tags (Bruno: montagem e reparos | Carla: aulas | Diego: limpeza e jardinagem | Felipe: reparos)
INSERT INTO tb_usuario_categoria VALUES ('1', '2');
INSERT INTO tb_usuario_categoria VALUES ('2', '2');
INSERT INTO tb_usuario_categoria VALUES ('6', '3');
INSERT INTO tb_usuario_categoria VALUES ('3', '4');
INSERT INTO tb_usuario_categoria VALUES ('4', '4');
INSERT INTO tb_usuario_categoria VALUES ('2', '6');

-- Pedido 1: ABERTO, da Ana, pago (daqui a 7 dias, 14h)
INSERT INTO tb_pedido (id_pedido, tb_categoria_id_categoria, tb_usuario_id_usuario, titulo_pedido, tipo_publicacao,
    status, descricao, dt_inicio, hora_inicio, valor, photo_url, nao_incluir_pagamento, latitude, longitude)
VALUES (TO_CHAR(sq_tb_pedido.NEXTVAL), '1', '1', 'Montar cama de casal', 'AJUDA', 'ABERTO',
    'Comprei uma cama de casal e preciso de ajuda para montar.', TRUNC(SYSDATE) + 7 + 14/24, TRUNC(SYSDATE) + 7 + 14/24,
    80, 'https://exemplo.com/cama.jpg', 'N', -23.56120000, -46.65580000);

-- Pedido 2: ACEITO, da Carla, com o Bruno escolhido (daqui a 3 dias, 10h)
INSERT INTO tb_pedido (id_pedido, tb_categoria_id_categoria, tb_usuario_id_usuario, titulo_pedido, tipo_publicacao,
    status, descricao, dt_inicio, hora_inicio, valor, nao_incluir_pagamento, latitude, longitude)
VALUES (TO_CHAR(sq_tb_pedido.NEXTVAL), '2', '3', 'Trocar chuveiro', 'AJUDA', 'ACEITO',
    'O chuveiro queimou e já comprei um novo.', TRUNC(SYSDATE) + 3 + 10/24, TRUNC(SYSDATE) + 3 + 10/24,
    60, 'N', -23.58900000, -46.63400000);

-- Pedido 3: ABERTO, da Carla, voluntário (sem pagamento, com recompensa)
INSERT INTO tb_pedido (id_pedido, tb_categoria_id_categoria, tb_usuario_id_usuario, titulo_pedido, tipo_publicacao,
    status, descricao, dt_inicio, hora_inicio, nao_incluir_pagamento, recompensa, latitude, longitude)
VALUES (TO_CHAR(sq_tb_pedido.NEXTVAL), '5', '3', 'Buscar remédio na farmácia', 'AJUDA', 'ABERTO',
    'Estou de repouso e preciso que alguém busque um remédio.', TRUNC(SYSDATE) + 2 + 9/24, TRUNC(SYSDATE) + 2 + 9/24,
    'S', 'Um bolo caseiro de agradecimento', -23.58900000, -46.63400000);

INSERT INTO tb_candidatura_pedido VALUES (TO_CHAR(sq_tb_candidatura_pedido.NEXTVAL), '2', '2', SYSDATE - 1, 'ESCOLHIDO', 'PENDENTE', NULL);

INSERT INTO tb_notificacao VALUES (TO_CHAR(sq_tb_notificacao.NEXTVAL), '1', '2', '3', 'NOVA_CANDIDATURA',
    'Bruno se candidatou ao seu pedido "Trocar chuveiro".', SYSDATE - 1, 'N');

COMMIT;

-- Conferência: 6 usuários, 6 categorias, 3 pedidos
SELECT id_usuario, nome, email FROM tb_usuario ORDER BY TO_NUMBER(id_usuario);
SELECT id_pedido, titulo_pedido, status, tb_usuario_id_usuario FROM tb_pedido ORDER BY TO_NUMBER(id_pedido);

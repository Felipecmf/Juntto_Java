# Juntto — API Java (Domain Driven Design Using Java)

API RESTful do **Juntto**, solução CitizenTech da Global Solution 2026/2 (FIAP · 1TDSPY). O Juntto conecta vizinhos: quem precisa de ajuda publica um pedido, quem pode ajudar se candidata, e as duas pessoas se avaliam no fim. A mesma conta pode pedir e oferecer ajuda.

Esta API é o back-end da solução. Ela guarda os dados no **Oracle da FIAP**, aplica as regras de negócio e é consumida pelo front-end em React.

## Links

| | |
|---|---|
| API publicada (Render) | https://juntto-java.onrender.com |
| Front-end (Vercel) | https://junttodevmode1.vercel.app |
| API de IA (Render) | https://junnto-ia.onrender.com |
| Vídeo no YouTube | https://youtu.be/rjgDZbgScEU |
| Repositório no GitHub | https://github.com/Felipecmf/Juntto_Java |
| Documentação técnica | `documentacao/Juntto_Documentacao_Tecnica_API_Java.pdf` |

> O Render gratuito "dorme" sem uso: a primeira requisição pode levar de 30 a 50 segundos.

## Integrantes — Turma 1TDSPY

| Nome | RM |
|---|---|
| Heloisa Costa Pereira | 572756 |
| Vitor Reis Souza | 573315 |
| Felipe Fernandes Cabral Mota | 569432 |
| Isabelle Dantas de Araujo | 570167 |
| Yasmin Nunes Santos | 568618 |

## Tecnologias

- Java 25 e Spring Boot (Web, Validation)
- JDBC puro com `DataSource` e `PreparedStatement` (sem JPA/Hibernate/Spring Data)
- Oracle Database (FIAP)
- ModelMapper (conversão DTO ↔ model)
- Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@Positive`, `@Future`...)
- Docker (Dockerfile multi-stage) e Render

## Arquitetura

```
Front-end (React/Vite, Vercel) ──HTTP + JSON──▶ API Spring Boot (Docker, Render) ──JDBC──▶ Oracle FIAP
```

Dentro da API, um pacote por responsabilidade (`src/main/java/br/com/juntto`):

| Pacote | Responsabilidade |
|---|---|
| `controller` | Rotas REST e status HTTP; recebe os DTOs com `@Valid` |
| `dto` | Entrada (`*Request`, com Bean Validation) e saída (`*Response`); o model nunca sai direto |
| `service` | Regras de negócio e orquestração dos DAOs |
| `dao` | Acesso ao Oracle com JDBC: um DAO por entidade, `DataSource` no construtor |
| `model` | Entidades do domínio e enums |
| `exception` | Exceções do domínio (`RegraNegocioException`, `RecursoNaoEncontradoException`, `DadosInvalidosException`, `AcessoNegadoException`) |
| `handler` | `@RestControllerAdvice` que devolve todo erro num JSON padronizado |
| `config` | CORS (`CorsFilter`) |
| `util` | `Conversor` (ModelMapper), `Senha` (hash) e `Validador` |

## Domínio

Entidades: **Usuario**, **Categoria**, **Pedido**, **Candidatura**, **Feedback** e **Notificacao**.

- **1:N** Usuario → Pedido (um usuário publica vários pedidos)
- **1:N** Pedido → Candidatura (um pedido recebe várias candidaturas)
- **N:N** Usuario ↔ Categoria (categorias de interesse, tabela `tb_usuario_categoria`)
- Candidatura → Feedback (1:1) e Candidatura → Notificacao (1:N)

Tipos de publicação: **AJUDA** (quem publica precisa de ajuda; um único ajudante) e **OFERTA** (quem publica oferece ajuda; vários interessados).

## Estrutura de pastas

```
Juntto_Java/
├── database/
│   └── script.sql          # tabelas, sequences e dados de teste (pode rodar de novo)
├── documentacao/
│   └── Juntto_Documentacao_Tecnica_API_Java.pdf
├── src/main/java/br/com/juntto/
│   ├── config/  controller/  dao/  dto/  exception/
│   ├── handler/  model/  service/  util/
│   └── JunttoApplication.java
├── src/main/resources/application.properties
├── Dockerfile
├── pom.xml
└── README.md
```

## Como rodar localmente

**Pré-requisitos:** Java 25, Maven 3.9+ e acesso ao Oracle da FIAP.

1. **Banco:** abra `database/script.sql` no SQL Developer, na sua conexão do Oracle FIAP, e execute com **F5**. Ele cria as tabelas, as sequences e os dados de teste.
2. **Variáveis de ambiente** (no IntelliJ: *Run → Edit Configurations → Environment variables*):

   | Variável | Exemplo de valor |
   |---|---|
   | `DB_URL` | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` |
   | `DB_USER` | seu RM no Oracle (ex.: `rm000000`) |
   | `DB_PASSWORD` | sua senha do Oracle |
   | `CORS_ORIGIN` | `http://localhost:5173` |

   As credenciais **não ficam no código**: o `application.properties` só lê essas variáveis.
3. **Executar:** rode `JunttoApplication` na IDE ou, no terminal:
   ```bash
   mvn spring-boot:run
   ```
4. **Testar:** abra http://localhost:8080/categorias no navegador.

**Com Docker:**
```bash
docker build -t juntto-api .
docker run -p 8080:8080 -e PORT=8080 -e DB_URL="..." -e DB_USER="..." -e DB_PASSWORD="..." -e CORS_ORIGIN="http://localhost:5173" juntto-api
```

**Usuários de teste** (criados pelo `script.sql`, senha `senha123`): `ana@email.com`, `bruno@email.com`, `carla@email.com`, `diego@email.com`, `elisa@email.com`, `felipe@email.com`.

> As datas dos pedidos de teste são calculadas a partir do dia em que o script roda. Rode de novo antes de apresentar.

## Deploy no Render

1. Web Service do tipo **Docker** ligado a este repositório (o `Dockerfile` compila com Maven e executa só o `.jar`).
2. Variáveis de ambiente do serviço (só os nomes): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `CORS_ORIGIN` (origens separadas por vírgula, sem barra no final) e `PORT` (informada pelo Render).
3. Cada novo commit gera uma nova versão em https://juntto-java.onrender.com.

## Endpoints

Todas as rotas recebem e devolvem JSON. As ações que alteram dados informam quem está agindo (`usuarioId` ou `idSolicitante`), e a API confere a permissão.

**Usuários e categorias**

| Método | Rota | Sucesso | O que faz |
|---|---|---|---|
| POST | `/usuarios` | 201 | Cadastro (e-mail único) |
| POST | `/login` | 200 | Login com e-mail e senha |
| GET | `/usuarios` · `/usuarios/{id}` | 200 | Perfis (com `?usuarioId=` do próprio id, todos os dados) |
| PUT | `/usuarios/{id}` | 200 | Edita o próprio perfil (`usuarioId` no corpo) |
| DELETE | `/usuarios/{id}?usuarioId=` | 204 | Exclui a própria conta |
| GET / PUT | `/usuarios/{id}/tags` | 200 | Categorias de interesse |
| GET | `/usuarios/{id}/pedidos?papel=SOLICITANTE\|PRESTADOR` | 200 | Histórico (rota aninhada 1:N) |
| GET / POST | `/categorias` | 200 / 201 | Lista / cria categoria |
| GET / PUT / DELETE | `/categorias/{id}` | 200 / 200 / 204 | Busca / renomeia / remove |

**Pedidos**

| Método | Rota | Sucesso | O que faz |
|---|---|---|---|
| POST | `/pedidos` | 201 | Publica um pedido de AJUDA ou uma OFERTA |
| GET | `/pedidos` | 200 | Oportunidades abertas (`?tipo=&idCategoria=&valorMin=&valorMax=&data=`) |
| GET | `/pedidos/{id}` | 200 | Detalhe do pedido |
| PUT | `/pedidos/{id}` | 200 | Edita (só o dono, antes de aceitar alguém) |
| DELETE | `/pedidos/{id}?usuarioId=` | 204 | Exclui (com candidatos, vira CANCELADO) |
| PATCH | `/pedidos/{id}/status` | 200 | Conclui ou cancela |
| PATCH | `/pedidos/{id}/pagamento/confirmacao` | 200 | Quem recebeu confirma o pagamento ou a recompensa |

**Candidaturas, avaliações e notificações**

| Método | Rota | Sucesso | O que faz |
|---|---|---|---|
| POST | `/pedidos/{id}/candidaturas` | 201 | Candidata-se (rota aninhada 1:N) |
| GET | `/pedidos/{id}/candidaturas?usuarioId=` | 200 | Dono do pedido vê os candidatos |
| GET | `/candidaturas?usuarioId=` · `/candidaturas/{id}?usuarioId=` | 200 | Minhas candidaturas / detalhe |
| PUT | `/candidaturas/{id}` | 200 | Dono aceita (`ACEITA`) ou recusa (`RECUSADA`) |
| DELETE | `/candidaturas/{id}?usuarioId=&confirmar=true` | 204 | Candidato desiste |
| POST / GET | `/pedidos/{id}/feedbacks` | 201 / 200 | Avalia o serviço concluído / avaliações do pedido |
| GET | `/feedbacks?usuarioId=` · `/feedbacks/{id}` | 200 | Avaliações recebidas / uma avaliação |
| PUT / DELETE | `/feedbacks/{id}` | 200 / 204 | Edita ou apaga a própria avaliação (até 7 dias) |
| GET | `/notificacoes?usuarioId=&lida=false` · `/notificacoes/{id}` | 200 | Notificações do usuário |
| POST | `/notificacoes` | 201 | Lembrete para a outra pessoa da candidatura |
| PUT / DELETE | `/notificacoes/{id}` | 200 / 204 | Marca como lida / apaga (só o destinatário) |

### Exemplo: publicar um pedido

```http
POST https://juntto-java.onrender.com/pedidos
Content-Type: application/json

{
  "idSolicitante": "3",
  "titulo": "Buscar remédio na farmácia",
  "descricao": "Estou de repouso e preciso que alguém busque um remédio.",
  "tipoPublicacao": "AJUDA",
  "idCategoria": "5",
  "naoIncluirPagamento": true,
  "recompensa": "Um bolo caseiro de agradecimento",
  "dtInicio": "2026-12-10T09:00:00",
  "latitude": -23.589,
  "longitude": -46.634
}
```

Resposta: **201 Created**, com o cabeçalho `Location` e o pedido criado (status `ABERTO`).

## Regras de negócio

- Ninguém se candidata ao próprio pedido, nem duas vezes ao mesmo pedido (409).
- Em **AJUDA**, só um candidato é aceito e o pedido passa sozinho para `EM_ANDAMENTO`; em **OFERTA**, vários interessados podem ser aceitos.
- Só o dono do pedido aceita candidatos, edita, exclui, conclui ou cancela (403 para os demais).
- Pedido só é editado ou excluído enquanto está `ABERTO` e sem ninguém aceito.
- Quem recebeu o pagamento o confirma; sem resposta em 48 horas, a confirmação é automática. A plataforma não movimenta dinheiro nem cobra comissão.
- Avaliação só depois da conclusão, uma por lado em cada candidatura aceita.
- Só o dono altera o próprio perfil e a própria conta, e só o destinatário altera a própria notificação.

## Erros

Todo erro volta no mesmo formato JSON, sem stack trace nem SQL:

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos. Verifique os campos.",
  "caminho": "/pedidos",
  "campos": ["titulo: O campo é obrigatório."],
  "timestamp": "2026-11-19T13:06:59"
}
```

| Status | Quando |
|---|---|
| 400 | Dados inválidos (Bean Validation) ou JSON malformado |
| 403 | Ação não permitida para este usuário |
| 404 | Id ou rota inexistente |
| 405 | Método não suportado na rota |
| 409 | Regra de negócio violada ou remoção de registro com filhos |
| 500 | Erro inesperado (mensagem genérica) |

## Uso de inteligência artificial

O grupo usou o **Claude (Anthropic)** como na adequação ao documento de banco e business, na explicação de erros (Oracle, CORS e deploy) e na organização da documentação. Todas as rotas foram testadas pelo grupo no Oracle FIAP e na API publicada.

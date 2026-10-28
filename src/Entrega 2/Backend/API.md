# API Próxima Etapa — Documentação

API REST em Node.js + Express + SQLite (banco de desenvolvimento próprio,
inspirado no inventário de banco de dados fornecido, com dados fictícios).

Base URL local: `http://localhost:3000/api`

Todas as rotas, exceto `/auth/login` e `/health`, exigem o cabeçalho:
```
Authorization: Bearer <token>
```
O token é obtido no login e representa o aluno autenticado (RFM01).

## Autenticação

| Método | Rota | Corpo | Descrição |
|---|---|---|---|
| POST | /auth/login | `{ "email", "senha" }` | Retorna `{ token, aluno }` |
| POST | /auth/logout | — | Stateless; o app apenas descarta o token |

Login de teste (dados fictícios, gerados pelo seed):
`ana.souza@aluno.proximaetapa.org.br` / senha `123456` (todos os 30 alunos fictícios usam essa senha).

## Perfil e página inicial

| Método | Rota | Descrição | RF |
|---|---|---|---|
| GET | /home | Resumo da jornada (próximas atividades, cursos, notificações) | RFM02 |
| GET | /perfil | Dados do aluno autenticado | RFM14 |
| PATCH | /perfil | Atualiza escola, série, cidade, avatar | RFM14 |
| GET | /card | Card digital do aluno | RFM08 |

## Cursos e agenda

| Método | Rota | Descrição | RF |
|---|---|---|---|
| GET | /cursos | Lista cursos ativos | RFM03 |
| GET | /cursos/:id | Detalhe do curso + atividades | RFM03 |
| GET | /cursos/inscritos/meus | Cursos em que o aluno está inscrito | RFM03 |
| POST | /cursos/:id/inscricao | Inscreve o aluno no curso | — |
| GET | /agenda | Agenda individual (todos os encontros dos cursos inscritos) | RFM05 |

## Presença (QR Code)

| Método | Rota | Corpo | Descrição | RF |
|---|---|---|---|---|
| POST | /presencas/checkin | `{ "qr_code" }` | Registra presença (bloqueia duplicidade) | RFM06 |
| GET | /presencas/historico | — | Histórico de presenças do aluno | RFM07 |

## Certificados

| Método | Rota | Descrição | RF |
|---|---|---|---|
| GET | /certificados | Certificados obtidos pelo aluno | RFM09 |

## Teste de perfil (orientação educacional/profissional)

| Método | Rota | Corpo | Descrição | RF |
|---|---|---|---|---|
| GET | /perfil-teste/perguntas | — | Perguntas + alternativas | RFM10 |
| POST | /perfil-teste/respostas | `{ "respostas": {"1": 3, "2": 7} }` | Calcula e salva o resultado | RFM10 |
| GET | /perfil-teste/resultados | — | Histórico de resultados | RFM10 |

> O resultado é apresentado apenas como orientação, nunca como diagnóstico
> psicológico ou clínico (conforme regra de negócio do projeto).

## Mensagens, notificações e chat

| Método | Rota | Descrição | RF |
|---|---|---|---|
| GET | /mensagens | Comunicados enviados pela Próxima Etapa | RFM11 |
| GET | /notificacoes | Notificações do aluno | RFM13 |
| PATCH | /notificacoes/:id/lida | Marca uma notificação como lida | RFM13 |
| PATCH | /notificacoes/lidas/todas | Marca todas como lidas | RFM13 |
| GET | /chat | Histórico do canal de dúvidas | RFM12 |
| POST | /chat | Envia mensagem no canal de dúvidas | RFM12 |

## Exemplo completo (curl)

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.souza@aluno.proximaetapa.org.br","senha":"123456"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 2. Listar cursos
curl -s http://localhost:3000/api/cursos -H "Authorization: Bearer $TOKEN"

# 3. Ver agenda
curl -s http://localhost:3000/api/agenda -H "Authorization: Bearer $TOKEN"
```

## Segurança e boas práticas aplicadas (RNF)

- Senhas com hash `bcrypt` (nunca texto puro).
- Autenticação via JWT (`Authorization: Bearer`), sessão sem estado no servidor.
- Todas as consultas usam *prepared statements* do `better-sqlite3` (parametrizadas), evitando SQL Injection.
- Chaves estrangeiras e `UNIQUE`/`CHECK` no schema garantem integridade (ex.: impede presença duplicada no mesmo encontro).
- CORS habilitado para o app consumir a API de outra origem.
- Variáveis sensíveis (`JWT_SECRET`, `PORT`) via `.env` (ver `.env.example`).
- Log de requisições via `morgan`.
- Dados 100% fictícios no banco de desenvolvimento (nenhum dado real de aluno).

## Rodando localmente

```bash
cd backend
npm install
npm run seed   # cria e popula o banco SQLite com dados fictícios
npm start      # sobe o servidor em http://localhost:3000
npm test       # roda o smoke test contra o servidor já em execução
```

---

# Entrega 2 — endpoints acrescentados

A Entrega 2 introduziu a **sincronização** (função principal da etapa), o
**check-in offline por QR Code** e o **módulo web administrativo**. Os
endpoints da Entrega 1 continuam valendo sem alteração.

## Sincronização (RFM15)

### `GET /api/sync`
Baixa, em uma requisição só, tudo que o app precisa guardar no celular.

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `desde` | query, opcional | Carimbo devolvido pelo servidor na última sincronização. Sem ele, vem o pacote completo. |

Resposta:
```json
{
  "servidor_em": "2026-10-27 18:40:12",
  "completo": true,
  "aluno": { "id": 1, "nome": "Ana Souza", "...": "..." },
  "cursos": [ ... ],
  "inscricoes": [ { "id": 3, "curso_id": 2, "status": "cursando" } ],
  "agenda": [ ... ],
  "presencas": [ ... ],
  "certificados": [ ... ],
  "mensagens": [ ... ],
  "notificacoes": [ ... ],
  "totais": { "cursos": 7, "agenda": 9, "...": 0 }
}
```

O app guarda `servidor_em` e o envia como `desde` na vez seguinte. Usamos o
relógio do **servidor**, e não o do celular: com a hora do aparelho errada, o
app pularia registros criados nesse intervalo.

### `POST /api/sync/presencas`
Sobe a fila de check-ins feitos sem internet.

```json
{
  "presencas": [
    { "uuid_local": "7b1e…", "qr_code": "PE-5A78…", "registrado_em": "2026-10-27 18:05:00" }
  ]
}
```

Resposta — cada item recebe seu próprio status, porque um QR inválido no meio
da fila não pode impedir que os outros entrem:

```json
{
  "servidor_em": "2026-10-27 18:41:03",
  "recebidas": 3,
  "sincronizadas": 2,
  "resultados": [
    { "uuid_local": "7b1e…", "status": "sincronizado", "atividade": "Encontro 1", "atividade_id": 4 },
    { "uuid_local": "9c2f…", "status": "duplicado", "erro": "Presença já sincronizada." },
    { "uuid_local": "a3d0…", "status": "qr_invalido", "erro": "QR Code inválido ou expirado." }
  ]
}
```

| Status | Significado | O que o app faz |
|---|---|---|
| `sincronizado` | Registrado agora | Remove da fila |
| `duplicado` | Já existia (reenvio ou leitura repetida) | Remove da fila |
| `nao_inscrito` | Aluno não está inscrito no curso | Remove da fila e avisa |
| `qr_invalido` | Código não corresponde a nenhum encontro | Remove da fila e avisa |
| `erro` | Falha temporária | Mantém na fila para a próxima tentativa |

O campo `uuid_local` é gerado no celular e torna o envio **idempotente**: se a
resposta se perder e o app reenviar, o servidor reconhece o mesmo
identificador em vez de gravar a presença duas vezes. O `registrado_em`
preserva o horário real do check-in — não o momento em que a internet voltou.

Limite de 200 itens por requisição. O lote inteiro roda em uma transação.

## Módulo web administrativo

Login separado do aluno. O token carrega `tipo: "admin"`, então um token de
aluno não abre o painel e um token de administrador não abre as rotas do app
(ambos respondem `403`).

| Método | Rota | Função |
|---|---|---|
| `POST` | `/api/admin/login` | Autentica a equipe da organização |
| `GET` | `/api/admin/metricas` | Números do painel (alunos, cursos, frequência média) |
| `GET` | `/api/admin/cursos` | Lista cursos com nº de inscritos e encontros |
| `POST` | `/api/admin/cursos` | Cria curso (notifica todos os alunos — RFM04) |
| `PATCH` | `/api/admin/cursos/:id` | Edita curso |
| `DELETE` | `/api/admin/cursos/:id` | Desativa o curso, preservando o histórico |
| `GET` | `/api/admin/cursos/:id/atividades` | Encontros do curso |
| `POST` | `/api/admin/cursos/:id/atividades` | Cria encontro e gera o QR Code |
| `GET` | `/api/admin/atividades/:id/qrcode.svg` | QR Code do encontro, em SVG, para projetar |
| `GET` | `/api/admin/atividades/:id/presencas` | Chamada: quem compareceu e quem faltou |
| `DELETE` | `/api/admin/atividades/:id` | Exclui encontro |
| `GET` | `/api/admin/alunos?busca=` | Lista/busca alunos |
| `GET` | `/api/admin/alunos/:id` | Ficha do aluno com frequência por curso |
| `GET` | `/api/admin/mensagens` | Comunicados enviados, com alcance e leituras |
| `POST` | `/api/admin/mensagens` | Envia comunicado (vira notificação no app — RFM13) |
| `GET` | `/api/admin/certificados/elegiveis?frequencia_minima=75` | Quem já cumpriu o critério |
| `POST` | `/api/admin/certificados` | Emite certificado e notifica o aluno |
| `GET` | `/api/admin/chat` | Conversas abertas |
| `GET` | `/api/admin/chat/:alunoId` | Histórico de uma conversa |
| `POST` | `/api/admin/chat/:alunoId` | Responde o aluno |

O QR Code de cada encontro é um código aleatório (`PE-` + 12 caracteres
hexadecimais), e não algo derivado do id: assim ninguém consegue adivinhar o
código do próximo encontro e registrar presença sem estar lá.

## Alterações em endpoints existentes

- **`GET`/`POST /api/chat`** — o histórico passou a ser gravado na tabela
  `chat_mensagens`. Na Entrega 1 ficava em memória e se perdia a cada
  reinício do servidor (o plano gratuito do Render reinicia sozinho).
- **Banco** — novas tabelas `administradores` e `chat_mensagens`; colunas
  `atualizado_em` (cursos, atividades, mensagens, inscrições) para o delta
  sync e `uuid_local` (presenças) para a idempotência.

## Como testar

```bash
npm install
npm run seed          # popula o banco e imprime QR Codes de exemplo
npm start             # http://localhost:3000 (painel) e /api (API)

# em outro terminal
node test-api.js      # bateria da Entrega 1 (11 verificações)
node test-api-e2.js   # bateria da Entrega 2 (34 verificações)
```

Credenciais de demonstração (fictícias): aluno
`ana.souza@aluno.proximaetapa.org.br` / `123456`; painel
`admin@proximaetapa.org.br` / `123456`.

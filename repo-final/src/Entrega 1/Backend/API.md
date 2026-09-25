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

# Projeto Interdisciplinar — Relatório de Implantação (Entrega 2)

**Projeto:** Próxima Etapa — App do Aluno
**Aluno:** Gustavo

> Requisito da etapa: *"aplicativo, servidor/backend, banco de dados e módulo
> web finalizados e integrados. Formato: código-fonte + APK/AAB + URL do
> servidor + documentação. Objetivo: implantar o sistema final em condições de
> demonstração."*

---

## 1. Os quatro componentes e como se ligam

```
┌─────────────────┐      HTTPS/JSON      ┌──────────────────────┐
│  App Android    │ ───────────────────▶ │  API REST            │
│  (Java + Room)  │ ◀─────────────────── │  Node.js + Express   │
└─────────────────┘   GET /sync          └──────────┬───────────┘
        │              POST /sync/presencas         │
        │                                           │
   banco local                                 ┌────▼─────┐
   (SQLite/Room)                               │  SQLite  │
   funciona offline                            └────▲─────┘
                                                    │
┌─────────────────┐                                 │
│  Módulo Web     │ ────────────────────────────────┘
│  (painel da ONG)│   cria cursos, encontros, QR Code,
└─────────────────┘   comunicados e certificados
```

O ciclo que fecha o sistema: **a ONG cria um encontro no painel web** → o
sistema gera o QR Code → **o aluno escaneia pelo app** → a presença sobe (na
hora ou quando a internet voltar) → **a chamada aparece no painel** → ao
atingir a frequência mínima, a equipe **emite o certificado** → o aluno recebe
uma notificação e vê o certificado no app.

---

## 2. Endereços e acesso

| Item | Onde |
|---|---|
| API | `https://proxima-etapa-api.onrender.com/api` |
| Verificação rápida | `https://proxima-etapa-api.onrender.com/api/health` |
| Painel administrativo | `https://proxima-etapa-api.onrender.com/` |
| Código-fonte | `https://github.com/2026-2-NCC3/Projeto8` |
| APK | esta mesma pasta (`app-debug.apk`) |

**Credenciais de demonstração — fictícias, criadas pelo script de seed:**

| Perfil | E-mail | Senha |
|---|---|---|
| Aluno | `ana.souza@aluno.proximaetapa.org.br` | `123456` |
| Painel da ONG | `admin@proximaetapa.org.br` | `123456` |

Os 30 alunos fictícios seguem o mesmo padrão de e-mail
(`nome.sobrenome@aluno.proximaetapa.org.br`) e usam a mesma senha.

---

## 3. Implantação

**Serviço:** Render (plano gratuito), implantação automática a cada push na
branch `main`.

| Configuração | Valor |
|---|---|
| Root Directory | `src/Entrega 2/Backend` |
| Build Command | `npm install` |
| Start Command | `npm run seed && npm start` |
| Variável de ambiente | `JWT_SECRET` |
| Porta | definida pelo Render (`process.env.PORT`) |

> **Atenção ao publicar a Entrega 2:** o serviço no Render ainda aponta para
> `src/Entrega 1/Backend`. É preciso alterar o *Root Directory* para
> `src/Entrega 2/Backend` nas configurações do serviço — sem isso, a versão no
> ar continua sendo a da Entrega 1, sem sincronização nem painel web.

**Por que `npm run seed && npm start`:** o plano gratuito não dá acesso a
terminal, então o banco é populado na inicialização. O script de seed apaga e
recria os dados, o que o torna seguro de rodar a cada reinício.

**Limitação conhecida e assumida:** no plano gratuito o disco é efêmero e a
instância hiberna após alguns minutos sem uso, levando de 30 a 50 segundos
para voltar. Por isso o app usa timeout de 60 segundos. Para uso real, o passo
seguinte seria um banco gerenciado (PostgreSQL) e uma instância que não
hiberna.

---

## 4. Banco de dados

SQLite, com 11 tabelas, criado a partir de `db/schema.sql`. A estrutura foi
baseada no inventário fornecido pela ONG e adaptada para o ambiente acadêmico.

| Tabela | Conteúdo |
|---|---|
| `alunos`, `administradores` | Pessoas (dois perfis de acesso separados) |
| `cursos`, `atividades` | Cursos e seus encontros (cada um com seu QR Code) |
| `inscricoes`, `presencas` | Vínculo do aluno e sua frequência |
| `certificados` | Emitidos por critério de frequência |
| `perguntas_perfil`, `alternativas_perfil`, `resultados_perfil` | Teste de orientação |
| `mensagens`, `notificacoes`, `chat_mensagens` | Comunicação com o aluno |

Integridade garantida no próprio banco: chaves estrangeiras com `ON DELETE
CASCADE`, `UNIQUE(aluno_id, atividade_id)` impedindo presença duplicada,
`UNIQUE(aluno_id, curso_id)` impedindo inscrição e certificado duplicados, e
`CHECK` nos campos de status.

Acrescentado na Entrega 2: colunas `atualizado_em` (base da sincronização
incremental, mantidas por *triggers*) e `uuid_local` nas presenças (base da
idempotência do envio offline).

---

## 5. Segurança

- Senhas com **hash bcrypt** — nem o servidor conhece a senha original.
- Autenticação por **JWT**; o token do aluno e o do administrador carregam
  tipos diferentes, então um não abre as rotas do outro (`403`).
- **Consultas parametrizadas** em todo o backend (proteção contra SQL
  Injection).
- Comunicação **HTTPS**; o app recusa tráfego em texto puro.
- Segredos por **variável de ambiente**, nunca no repositório.
- QR Code de cada encontro é **aleatório**, não derivado do id — ninguém
  adivinha o código do próximo encontro para registrar presença sem estar lá.

---

## 6. Testes

| Bateria | Cobertura | Resultado |
|---|---|---|
| `node test-api.js` | 11 verificações da Entrega 1 | 11/11 |
| `node test-api-e2.js` | 34 verificações da Entrega 2 | 34/34 |

A bateria da Entrega 2 cobre o que foi acrescentado nesta etapa: pacote
completo e incremental de sincronização, separação entre token de aluno e de
administrador, criação de curso e encontro pelo painel aparecendo no app,
geração do QR Code, check-in válido, reenvio idempotente, QR inválido no meio
do lote, preservação do horário original do check-in, comunicado virando
notificação e chat persistido nos dois sentidos.

O módulo web foi percorrido em navegador (Playwright + Chromium) em todo o
fluxo — login, painel, criação de curso e encontro, projeção do QR Code,
chamada, busca de alunos, comunicado, certificados e chat — sem nenhum erro de
console. As capturas estão em `imagens/painel-*.png`.

---

## 7. Como demonstrar em 4 minutos

1. **Acordar o servidor** alguns minutos antes: abrir
   `…/api/health` no navegador.
2. **Painel web:** mostrar o número de frequência média → abrir um curso →
   criar um encontro → **projetar o QR Code**.
3. **App:** fazer login → tela inicial → **ativar o modo avião** → escanear o
   QR → o app confirma a presença e avisa que está aguardando conexão.
4. **Desligar o modo avião** → voltar à tela inicial → a fila sobe sozinha e o
   aviso some.
5. **Painel:** abrir a chamada daquele encontro — o aluno aparece como
   presente, com o **horário real do check-in**, não o da sincronização.
6. **Fechar o ciclo:** emitir um certificado no painel e mostrá-lo chegando
   como notificação no app.

---

## 8. O que ficaria para uma próxima etapa

- Banco gerenciado (PostgreSQL) e instância sem hibernação.
- Notificações push (Firebase) em vez de avisos gerados na sincronização.
- Autenticação com refresh token e `EncryptedSharedPreferences`.
- Publicação do app na Play Store.
- Tratamento de dados reais sob LGPD: consentimento dos responsáveis
  (o público inclui adolescentes), política de privacidade e retenção.

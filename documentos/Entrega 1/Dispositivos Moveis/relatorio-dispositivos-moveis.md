# Dispositivos Móveis — Relatório da Entrega 1

**Disciplina:** Programação para Dispositivos Móveis
**Projeto:** Próxima Etapa — App do Aluno
**Aluno:** Gustavo

## Objetivo

Desenvolver o aplicativo Android que consome a API REST do backend do
projeto, cobrindo o fluxo principal do aluno: login, consulta de cursos e
acompanhamento da agenda de atividades.

## O que foi desenvolvido

O app foi construído em **Java**, no Android Studio, e implementa o fluxo
**Login → Cursos → Detalhe do Curso → Agenda**:

- **Login** (`LoginActivity`): autentica contra `POST /api/auth/login` e
  guarda o token JWT retornado em `SharedPreferences`, via `SessionManager`.
- **Lista de cursos** (`CursosActivity` + `CursoAdapter`): exibe os cursos
  disponíveis em uma `RecyclerView`, um item por curso.
- **Detalhe do curso** (`CursoDetalheActivity`): aberto ao tocar em um item
  da lista, usando `Intent` com extras para passar os dados do curso
  selecionado.
- **Agenda** (`AgendaActivity` + `AgendaAdapter`): mostra os encontros das
  atividades em que o aluno está inscrito, também em `RecyclerView`.

## Componentes e tecnologias exigidos pelo documento do projeto

| Exigido | Onde foi usado |
|---|---|
| `Activities` | 4 Activities: Login, Cursos, CursoDetalhe, Agenda |
| `Intents` | Navegação entre `CursosActivity` → `CursoDetalheActivity`, passando o curso selecionado como extra |
| `ConstraintLayout` | Layout base de todas as telas (`activity_login.xml`, `activity_cursos.xml`, `activity_curso_detalhe.xml`, `activity_agenda.xml`) |
| `RecyclerView` | Lista de cursos (`item_curso.xml`) e lista de agenda (`item_agenda.xml`) |
| Dado numérico | Carga horária (`txtCargaHoraria`) e vagas disponíveis (`txtVagas`) exibidos em cada item de curso |

**Sobre `Fragments`:** o documento do projeto cita Fragments na lista de
componentes possíveis, mas como o fluxo desta entrega é composto por telas
completas e sequenciais (não por conteúdo alternável dentro de uma mesma
tela), optou-se por implementar cada etapa como uma `Activity` própria,
navegando por `Intents`. Essa é uma escolha de arquitetura equivalente e
comum em apps Android — Fragments ficam reservados para a Entrega 2, onde
telas com abas/conteúdo dinâmico (ex.: card digital, mensagens) fazem mais
sentido com esse componente.

## Integração com o backend

O app consome a API publicada em produção no Render
(`https://proxima-etapa-api.onrender.com/api/`), configurada em
`API_BASE_URL` no `app/build.gradle`. Todas as chamadas usam Retrofit
(`ApiService`) com o token JWT anexado automaticamente após o login.

## Como testar

1. Abrir a pasta `src/Entrega 1/Frontend` no Android Studio.
2. Aguardar a sincronização do Gradle.
3. Rodar em um emulador ou instalar o APK gerado (disponível em
   `documentos/Entrega 1/Projeto Interdisciplinar (Aplicativo Movel)/app-debug.apk`).
4. Fazer login com um dos usuários fictícios do seed, por exemplo:
   `ana.souza@aluno.proximaetapa.org.br` / `123456`.

## Próximos passos (Entrega 2)

- Check-in de presença por QR Code (biblioteca ZXing já incluída no `build.gradle`).
- Persistência local com Room e sincronização após perda de conexão.
- Uso de `Fragments` nas telas de card digital, teste de perfil e mensagens.

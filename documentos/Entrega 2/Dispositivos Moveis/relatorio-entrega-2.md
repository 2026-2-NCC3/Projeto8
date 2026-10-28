# Dispositivos Móveis — Relatório da Entrega 2

**Disciplina:** Programação para Dispositivos Móveis
**Projeto:** Próxima Etapa — App do Aluno
**Aluno:** Gustavo

---

## 1. O que a Entrega 2 pedia

> "Ampliar a Entrega 1 para consumir dados de uma API REST do projeto,
> integrando autenticação e armazenamento local. **Nova função principal:
> Sincronização dos dados do estudante e registro de presença por QR Code com
> persistência local e integração com a API.** Fluxo principal: Login →
> Cursos → Agenda → Dados da API → QR Code/Check-in → Persistência →
> Sincronização → Histórico. Implementar tratamento de JSON, SQLite/Room,
> notificações, histórico, perda de conexão, card, certificados, teste de
> perfil e mensagens/chat."

Tudo isso foi implementado. O quadro abaixo diz onde cada item está no código.

| Item exigido | Onde está |
|---|---|
| Consumo da API REST | `data/ApiService.java` (24 endpoints), `data/ApiClient.java` |
| Autenticação integrada | `ui/LoginActivity.java` + interceptor no `ApiClient` |
| Armazenamento local | `data/local/` — Room com 8 entidades |
| **Sincronização** | `data/Repositorio.java` + `GET /api/sync` |
| **QR Code / check-in** | `ui/CheckinActivity.java` (ZXing) + `POST /api/sync/presencas` |
| Persistência do check-in | `data/local/PresencaPendente.java` (fila offline) |
| Tratamento de JSON | Gson, via `converter-gson` do Retrofit |
| Notificações | `util/Notificador.java` + `ui/NotificacoesActivity.java` |
| Histórico | `ui/HistoricoActivity.java` |
| Perda de conexão | `Repositorio.temConexao()` + avisos em cada tela |
| Card digital | `ui/CardActivity.java` |
| Certificados | `ui/CertificadosActivity.java` |
| Teste de perfil | `ui/TestePerfilActivity.java` + `ui/PerguntaAdapter.java` |
| Mensagens e chat | `ui/MensagensActivity.java`, `ui/ChatActivity.java` |

---

## 2. A decisão central: offline primeiro

O app atende jovens de escolas públicas, e os encontros acontecem em escolas e
espaços comunitários onde o sinal falha com frequência. Isso deixou de ser
detalhe técnico e virou a decisão de arquitetura da entrega:

> **Nenhuma tela consulta a API para se desenhar.** Todas leem do banco local
> (Room). A rede serve apenas para atualizar esse banco, em segundo plano.

Consequências práticas:

- O app abre preenchido e instantâneo, mesmo sem internet.
- Uma falha de rede nunca deixa a tela vazia — no máximo desatualizada, e a
  tela diz isso.
- A presença do aluno não se perde quando o sinal cai justamente no encontro.

Três telas abrem exceção, e cada uma explica o porquê ao aluno: **inscrição**
(depende de vaga disponível naquele instante), **chat** (uma pergunta
guardada no celular não chega a ninguém) e **teste de perfil** (o resultado é
calculado no servidor).

---

## 3. A fila de check-ins offline

É a peça que resolve o problema real da entrega. Frequência é o que decide se
o aluno recebe certificado — uma presença perdida por falta de sinal custa
caro para ele.

**Como funciona**

1. O aluno escaneia o QR Code do encontro.
2. A presença é gravada **na hora** na tabela `presencas_pendentes`, com o
   horário real do celular, e o aluno já vê a confirmação.
3. O app tenta enviar. Se não houver rede, o item fica na fila.
4. Quando a conexão volta, a fila sobe em ordem de chegada (FIFO).
5. Cada item só sai da fila depois que o servidor confirma.

**Idempotência.** Cada check-in carrega um `uuid_local` gerado no aparelho. Se
a resposta do servidor se perder no caminho e o app reenviar, o servidor
reconhece o mesmo identificador e não grava a presença duas vezes. Sem isso, a
rede instável — exatamente o cenário que a fila existe para resolver —
produziria presenças duplicadas.

**Ordem da sincronização.** O app **sobe a fila antes de baixar** os dados. Se
baixasse primeiro, a agenda recém-recebida ainda não conheceria as presenças
pendentes e o aluno veria como ausente um encontro ao qual compareceu.

**Status por item.** O servidor responde item a item, e não o lote inteiro:
um QR inválido no meio da fila não pode impedir que os outros entrem.

| Status | O que o app faz |
|---|---|
| `sincronizado` | Remove da fila |
| `duplicado` | Remove da fila (já estava registrado) |
| `nao_inscrito` / `qr_invalido` | Remove e avisa — reenviar não mudaria o resultado |
| `erro` | Mantém na fila para a próxima tentativa |

---

## 4. Sincronização incremental

`GET /api/sync` traz tudo o que o aluno precisa em **uma requisição**: perfil,
cursos, inscrições, agenda, presenças, certificados, comunicados e
notificações.

A resposta sempre inclui `servidor_em`. O app guarda esse carimbo e o envia
como `?desde=` na vez seguinte, recebendo de volta **somente o que mudou** —
menos dados móveis e menos bateria, que importam para esse público.

O carimbo é o do **servidor**, não o do celular. Com a hora do aparelho
errada — algo comum —, usar o relógio local faria o app pular registros
criados nesse intervalo.

---

## 5. Banco local (Room)

| Tabela | Para quê |
|---|---|
| `aluno` | Perfil e card abrirem offline |
| `cursos` | Lista de cursos e situação da inscrição |
| `agenda` | Encontros do aluno |
| `presencas` | Histórico confirmado pelo servidor |
| **`presencas_pendentes`** | **Fila de check-ins offline** |
| `certificados` | Certificados emitidos |
| `notificacoes` | Avisos, com marcação de lida e de já exibida |
| `mensagens` | Comunicados da organização |

Duas decisões que valem registro:

- **Escrita por substituição** (`OnConflictStrategy.REPLACE`): a
  sincronização recebe a versão autoritativa do servidor e sobrescreve a cópia
  local. A única tabela fora dessa regra é a fila de pendentes, que é escrita
  pelo próprio aluno e só pode ser apagada após confirmação.
- **Campo `exibida` nas notificações**: separa "o aluno leu" de "o app já
  avisou na barra de status". Sem essa distinção, cada sincronização
  reexibiria os mesmos avisos.

---

## 6. Componentes Android exigidos pelo documento

| Componente | Onde |
|---|---|
| `Activities` | 13 telas |
| `Intents` | Navegação e passagem do curso selecionado |
| `ConstraintLayout` | Layout base de todas as telas |
| `RecyclerView` | Cursos, agenda, histórico, certificados, comunicados, notificações, chat e perguntas |
| `TextView`, `Button`, `ImageView` | Em todas as telas |
| Dado numérico | Carga horária e vagas nos cursos; contadores de presença na tela inicial |

**Sobre `Fragments`:** o documento os cita entre os componentes possíveis. O
fluxo desta entrega é de telas completas e sequenciais, então cada etapa é uma
`Activity` com navegação por `Intent` — escolha equivalente e comum em
Android. Onde Fragments realmente agregariam (abas dentro de uma mesma tela),
o app ainda não tem esse tipo de navegação.

---

## 7. Tratamento de erros e estados

Toda tela trata quatro situações, em vez de apenas o caminho feliz:

- **Carregando** — barra de progresso ou indicador de atualização.
- **Lista vazia** — texto explicando o que fazer, não uma tela em branco.
- **Offline** — faixa âmbar avisando que os dados podem estar desatualizados.
  Âmbar, e não vermelho: estar offline é uma situação prevista, não uma falha.
- **Sessão expirada** — o app avisa e volta ao login em vez de dar erro.

O timeout de rede subiu de 15 para 60 segundos por um motivo concreto
encontrado em uso real: o servidor roda no plano gratuito do Render, que
hiberna após alguns minutos sem uso e leva de 30 a 50 segundos para voltar.
Com 15 segundos, a primeira requisição do dia falhava sempre — o servidor
respondia, mas o app já tinha desistido.

---

## 8. Privacidade e LGPD

- Ao sair da conta, **todos** os dados locais são apagados: é comum um mesmo
  aparelho ser usado por mais de uma pessoa.
- O app só fala com a API por **HTTPS** (`usesCleartextTraffic="false"`).
- O token fica em `SharedPreferences` privado do app; o próximo passo seria
  `EncryptedSharedPreferences`.
- Todos os dados da demonstração são **fictícios**, gerados pelo script de
  seed. Nenhum dado real de aluno da Próxima Etapa foi usado.

---

## 9. Como verificar

```bash
# App
Abrir src/Entrega 2/Frontend no Android Studio → Run ▶
Login: ana.souza@aluno.proximaetapa.org.br / 123456

# Demonstrar o modo offline
Ativar o modo avião ANTES de escanear o QR Code:
o app confirma a presença e mostra "aguardando conexão";
ao voltar a conexão, a fila sobe sozinha.
```

O APK está em
`documentos/Entrega 2/Projeto Interdisciplinar (Aplicativo Movel)/`.

# Próxima Etapa — App Android (Entrega 2)

Projeto Android Studio (Java) com o fluxo completo da Entrega 2:

**Login → Início → Cursos → Agenda → Dados da API → QR Code/Check-in →
Persistência → Sincronização → Histórico**

## O que mudou em relação à Entrega 1

| Entrega 1 | Entrega 2 |
|---|---|
| Telas liam direto da rede | Telas leem do **banco local (Room)**; a rede só atualiza esse banco |
| Sem presença | **Check-in por QR Code (ZXing)** com fila offline |
| Sem persistência | **8 tabelas Room**, incluindo a fila de check-ins pendentes |
| 4 telas | **13 telas**, cobrindo RFM01 a RFM15 |
| Uma chamada por tela | **`GET /sync`**: um pacote só, incremental |
| Timeout de 15 s | 60 s — o plano gratuito do Render hiberna e leva até 50 s para acordar |

## Arquitetura

```
data/
  ApiClient.java        Retrofit + token + timeouts
  ApiService.java       contrato da API
  SessionManager.java   token e carimbo da última sincronização
  Repositorio.java      ponte Room ↔ API (offline primeiro)
  local/
    AppDatabase.java    banco Room
    DadosDao.java       todas as consultas
    *Local.java         7 entidades espelhando a API
    PresencaPendente.java   a fila de check-ins offline
model/    objetos que viajam pela rede (JSON)
ui/       13 Activities + 5 Adapters
util/     Notificador (avisos) e Formato (datas)
```

### A regra que organiza tudo: offline primeiro

Nenhuma tela chama a API para desenhar a si mesma. Todas leem do banco local,
e a sincronização atualiza esse banco em segundo plano. É por isso que o app
abre instantaneamente e continua funcionando no meio de um encontro sem sinal
— que é a situação real nos espaços onde a ONG atua.

As três exceções são deliberadas, e cada tela diz isso ao aluno quando
acontece: **inscrição** (depende de vaga disponível agora), **chat** (uma
pergunta guardada no celular não chega a ninguém) e **teste de perfil** (o
resultado é calculado no servidor).

### A fila de sincronização

É o coração da entrega. O aluno escaneia o QR Code; a presença vai primeiro
para a tabela `presencas_pendentes`, com o horário real do celular, e só
depois o app tenta enviá-la. Quando a conexão volta, a fila sobe em ordem de
chegada (FIFO) e cada item só é apagado depois que o servidor confirma.

Cada item carrega um `uuid_local` gerado no aparelho. Se a resposta do
servidor se perder no caminho e o app reenviar, o servidor reconhece o mesmo
identificador e não registra a presença duas vezes — é o que torna o reenvio
seguro.

A sincronização **sobe antes de baixar**. Se baixasse primeiro, a agenda
recém-recebida ainda não conheceria as presenças da fila e o aluno veria como
ausente um encontro ao qual compareceu.

## Como abrir e rodar

1. Abra a pasta `src/Entrega 2/Frontend` no **Android Studio** (não a raiz do
   repositório).
2. Aguarde a sincronização do Gradle.
3. Rode no emulador ou em um celular (**Run ▶**).
4. Entre com um dos logins fictícios: `ana.souza@aluno.proximaetapa.org.br` /
   `123456`.

O backend já está publicado, então **não é preciso rodar nada localmente**. A
URL fica em `app/build.gradle` (`API_BASE_URL`).

> O servidor roda no plano gratuito do Render, que hiberna após alguns
> minutos sem uso. A primeira requisição do dia pode levar de 30 a 50
> segundos — abra o app um pouco antes de apresentar.

### Testando o check-in por QR Code

1. Abra o painel administrativo (a URL do servidor, na raiz) e entre com
   `admin@proximaetapa.org.br` / `123456`.
2. Vá em um curso → um encontro → **Ver QR Code**.
3. No app, toque em **Registrar presença** e aponte a câmera para a tela.

Para demonstrar o modo offline: ative o modo avião **antes** de escanear. O
app confirma a presença na hora e mostra "aguardando conexão"; ao desligar o
modo avião e voltar à tela inicial, a fila sobe sozinha.

### Gerando o APK

**Build → Build Bundle(s)/APK(s) → Build APK(s)**. O arquivo sai em
`app/build/outputs/apk/debug/app-debug.apk`.

## Permissões e por quê

| Permissão | Para quê |
|---|---|
| `INTERNET` | Falar com a API |
| `ACCESS_NETWORK_STATE` | Saber se está online antes de tentar, em vez de esperar o timeout |
| `CAMERA` | Ler o QR Code (opcional: o app instala em aparelhos sem câmera) |
| `POST_NOTIFICATIONS` | Avisos na barra de status (Android 13+) |

A permissão de câmera é pedida só quando o aluno toca em escanear, e a de
notificação só depois que ele já conhece o app — pedir tudo na abertura é o
caminho mais curto para a recusa.

## Privacidade

Ao sair da conta, o app apaga todos os dados locais (`Repositorio.limparTudo`),
porque é comum um mesmo aparelho ser usado por mais de uma pessoa. Todos os
dados usados na demonstração são fictícios, gerados pelo script de seed.

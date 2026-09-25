# Próxima Etapa — App Android (protótipo)

Projeto Android Studio (Java) com o fluxo **Login → Cursos → Agenda**, exigido
como função principal da Entrega 1 da UC "Programação para Dispositivos Móveis".

## O que já funciona

- **Login** (RFM01) consumindo `POST /api/auth/login`, com token salvo em `SharedPreferences`.
- **Lista de cursos** (RFM03) em `RecyclerView`, mostrando um dado numérico (carga horária) por item.
- **Detalhe do curso** ao tocar em um item (Activity + Intent com extras).
- **Agenda individual** (RFM05) com os encontros dos cursos em que o aluno está inscrito.
- Tratamento básico de erros de rede e de credenciais inválidas.

Usa `Activities`, `Intents`, `Fragments`-ready structure, `ConstraintLayout`,
`TextView`, `Button`, `RecyclerView`, conforme pedido no documento do projeto.
As dependências de QR Code (ZXing) e Room já estão no `build.gradle` para os
próximos incrementos (check-in por QR Code e persistência local — Entrega 2).

## Como abrir e rodar

Este ambiente de nuvem **não tem o Android SDK/emulador instalado**, então o
código foi escrito e revisado aqui, mas não pôde ser compilado neste
container. Para rodar:

1. Abra a pasta `android-app/` no **Android Studio** (Hedgehog ou mais recente).
2. Deixe o Gradle sincronizar (ele vai baixar as dependências do `app/build.gradle`).
3. Rode o backend localmente (`cd ../backend && npm install && npm run seed && npm start`).
4. Se for testar no **emulador**, a URL da API já está certa por padrão
   (`http://10.0.2.2:3000/api/`, que é como o emulador enxerga o `localhost`
   da sua máquina).
5. Se for testar em **celular físico**, troque `API_BASE_URL` em
   `app/build.gradle` pelo IP da sua máquina na rede Wi-Fi (ex.: `http://192.168.0.10:3000/api/`).
6. Rode o app (▶). Use um dos logins fictícios do seed, ex.:
   `ana.souza@aluno.proximaetapa.org.br` / `123456`.
7. Gere o launcher icon definitivo pelo assistente **Image Asset** do Android
   Studio (botão direito em `res` → New → Image Asset) — os ícones incluídos
   aqui são um placeholder simples em vetor.

## Estrutura

```
app/src/main/java/br/org/proximaetapa/app/
  data/     ApiClient, ApiService (Retrofit), SessionManager (token)
  model/    Aluno, Curso, AgendaItem, LoginRequest/Response
  ui/       LoginActivity, CursosActivity, AgendaActivity, CursoDetalheActivity + Adapters
```

## Próximos passos (Entrega 2)

- Check-in de presença por QR Code (ZXing já incluído nas dependências).
- Persistência local com Room + sincronização após reconexão (RFM15).
- Telas de card digital, certificados, teste de perfil, mensagens/chat e notificações.

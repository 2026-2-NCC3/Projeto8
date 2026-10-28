# Como gerar o APK da Entrega 2

O APK precisa ser gerado a partir do código da **Entrega 2**
(`src/Entrega 2/Frontend`), que é diferente do da Entrega 1: tem Room,
QR Code, sincronização e as nove telas novas.

> O APK que está em `documentos/Entrega 1/Projeto Interdisciplinar
> (Aplicativo Movel)/app-debug.apk` é o da **Entrega 1** e não serve para esta
> etapa.

## Passo a passo

1. Abra o **Android Studio**.
2. **File → Open** e selecione a pasta `src/Entrega 2/Frontend`
   (não a raiz do repositório, nem a pasta `app`).
3. Aguarde o Gradle sincronizar. Como as dependências do Room e do ZXing
   passaram a ser usadas de fato nesta entrega, a primeira sincronização baixa
   mais coisas que antes e pode demorar alguns minutos.
4. **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
5. Quando terminar, clique em **locate** na notificação. O arquivo estará em:
   ```
   src/Entrega 2/Frontend/app/build/outputs/apk/debug/app-debug.apk
   ```
6. Copie esse `app-debug.apk` para **esta pasta** e faça commit.

```bash
git add "documentos/Entrega 2/Projeto Interdisciplinar (Aplicativo Movel)/app-debug.apk"
git commit -m "Adiciona APK da Entrega 2"
git push
```

## Antes de testar

Confirme que o backend da Entrega 2 está publicado: o serviço no Render
precisa estar com o *Root Directory* apontando para `src/Entrega 2/Backend`.
Enquanto ele apontar para a Entrega 1, o app não encontra as rotas de
sincronização (`/api/sync`) e o painel web não abre.

Verificação rápida — esta URL deve responder `"versao":"2.0"`:

<https://proxima-etapa-api.onrender.com/api/health>

## Testando o fluxo offline (o ponto central da entrega)

1. Faça login no app.
2. Abra o painel web e projete o QR Code de um encontro.
3. **Ative o modo avião no celular.**
4. Escaneie o QR Code: o app confirma a presença na hora e avisa que está
   aguardando conexão.
5. Desligue o modo avião e volte à tela inicial: a fila sobe sozinha.
6. No painel, abra a chamada daquele encontro — a presença aparece com o
   horário em que foi feita, não com o da sincronização.

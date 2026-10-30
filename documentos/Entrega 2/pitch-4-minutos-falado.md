# Pitch de 4 minutos — versão falada (sem slide, sem demonstração)

**Projeto:** Próxima Etapa — App do Aluno

> Versão para treinar a apresentação sem nenhum apoio visual.
>
> **São 509 palavras faladas.** Num ritmo de apresentação normal — cerca de
> 135 palavras por minuto —, isso fecha em **3min58**, já contando as três
> pausas marcadas. Sobra margem para os 4 minutos.
>
> Se você fala devagar, estoura: a 110 palavras por minuto dá 4min50. Antes de
> decorar qualquer coisa, faça o teste de ritmo da seção "Como ensaiar" — são
> 30 segundos e diz exatamente em que velocidade você está.
>
> Para a apresentação valendo nota, que pede demonstração e banner, use o
> `pitch-4-minutos.md`.

---

## O roteiro

### 0:00 – 0:40 · Comece por uma pessoa

> "A Próxima Etapa é uma ONG que leva jovens de escola pública ao ensino
> superior, com cursos gratuitos.
>
> Pensa na Ana. Dezessete anos, terceiro ano do ensino médio. Ela se inscreve
> num curso de redação para o ENEM e vai a todos os encontros.
>
> No fim, ela precisa de setenta e cinco por cento de presença para receber o
> certificado — aquele que vai no currículo e na inscrição de bolsa.
>
> Hoje, essa presença é controlada numa lista de papel que circula pela sala."

*(Pausa. Deixe a imagem da lista de papel assentar.)*

---

### 0:40 – 1:20 · O tamanho do problema, com números

> "A gente analisou os dados do projeto e encontrou três coisas.
>
> Metade dos alunos — quarenta e nove vírgula oito por cento — fica abaixo dos
> setenta e cinco por cento de presença. Metade não recebe certificado.
>
> Frequência é a variável mais instável que medimos: coeficiente de variação
> de quarenta e nove por cento. Não existe 'o aluno médio'. Existe quem vai a
> tudo, e quem some no meio do caminho.
>
> E a chance de faltar a um encontro qualquer é de trinta e oito por cento.
>
> A ONG perde aluno no percurso e só descobre quando o curso acabou e o
> certificado não saiu."

---

### 1:20 – 1:45 · A solução, direto

> "A gente construiu o sistema inteiro dessa jornada: um aplicativo Android
> para o aluno, uma API, um banco de dados e um painel web para a equipe da
> ONG — onde ela cria os encontros, acompanha a frequência e emite os
> certificados.
>
> E a presença deixa de ser papel: vira um QR Code, registrado em dois
> segundos."

---

### 1:45 – 2:45 · A cena que define o projeto

*(Esta parte substitui a demonstração. Fale mais devagar.)*

> "Só que tem um detalhe que mudou o projeto inteiro.
>
> Esses encontros não acontecem num escritório. Acontecem em escola pública,
> em espaço comunitário, em sala emprestada. Lugares onde o sinal cai.
>
> E aí veio a pergunta que define esse sistema: o que acontece com a presença
> da Ana se, na hora do check-in, o celular dela estiver sem internet?
>
> Na maioria dos aplicativos, dá erro de conexão e a presença se perde. E
> presença perdida, aqui, é certificado perdido.
>
> Então a gente inverteu a lógica. Quando a Ana escaneia o QR Code, a presença
> é gravada primeiro no celular dela, na hora, com o horário real. Ela já vê
> 'presença confirmada'. O aplicativo envia sozinho quando ela pega Wi-Fi.
>
> E no painel da ONG, a presença aparece com o horário em que ela escaneou.
> Não o horário em que a internet voltou."

*(Pausa de dois segundos. É a frase mais importante do pitch.)*

---

### 2:45 – 3:15 · Uma decisão técnica que mostra profundidade

*(**Este é o bloco que você corta se estiver atrasado.** Decida ao chegar
aqui: se já passou de 2min50, pule direto para o fechamento — o pitch fecha
bem sem ele.)*

> "Isso criou um problema novo.
>
> Se o celular envia e a resposta do servidor se perde no caminho — que é o
> que acontece em rede ruim —, o aplicativo tenta de novo. E duas tentativas
> poderiam virar duas presenças no mesmo encontro.
>
> Então cada check-in leva um identificador único, gerado no próprio celular.
> O servidor reconhece a repetição e registra uma vez só."

---

### 3:15 – 3:50 · Feche voltando para a Ana

> "O sistema está no ar: quatro componentes integrados, quarenta e cinco
> testes automatizados passando, código aberto no GitHub. E serve para
> qualquer ONG ou cursinho que precise comprovar frequência.
>
> Mas o que ele resolve é bem específico: garantir que o esforço de uma aluna
> que atravessou a cidade para estar naquele encontro não se perca por causa
> de um sinal de celular.
>
> Obrigado."

---

## Como ensaiar

**Antes de tudo, descubra seu ritmo.** Leia só a primeira seção (0:00–0:40)
em voz alta, cronometrada. São 83 palavras. Se você levou:

| Tempo na 1ª seção | Seu ritmo | O pitch inteiro vai dar |
|---|---|---|
| ~33s | rápido (150/min) | 3min36 — dá para falar mais pausado |
| ~37s | normal (135/min) | 3min58 — no ponto |
| ~45s ou mais | devagar (110/min) | 4min50 — **corte o bloco 2:45–3:15** |

**Primeira passada completa:** leia tudo em voz alta, cronometrando, sem se
preocupar com mais nada. Anote onde passou ou sobrou do tempo.

**Segunda passada:** marque os três pontos onde você vai **parar de
propósito** — depois da lista de papel, depois do "não o horário em que a
internet voltou", e antes do "Obrigado". Pausa é o que separa quem apresenta
de quem recita.

**Terceira passada:** feche o papel e conte com suas palavras, olhando para a
parede. Se você lembrar da sequência — pessoa, números, solução, a cena do
offline, o detalhe técnico, fechamento —, o texto exato não importa mais.

## Três coisas que fazem diferença sem slide

1. **Diga os números por extenso e devagar.** "Quarenta e nove vírgula oito
   por cento" dá tempo de processar; "49,8%" lido rápido passa batido. Sem
   slide, o número só existe se você der espaço para ele.

2. **Volte à Ana no fim.** Você abriu com ela; fechar com ela amarra os quatro
   minutos numa coisa só.

3. **Não narre o aplicativo.** Sem demonstração, a tentação é descrever telas
   — "aí tem a tela de cursos, aí tem a agenda". Isso esvazia o pitch. Conte o
   problema e a decisão; a lista de funcionalidades você dá se perguntarem.

## Ajuste fino de tempo

**Se faltar tempo:** corte a seção 2:45–3:15 inteira (o identificador único).
É o trecho mais técnico e o menos essencial para quem ouve — o pitch continua
redondo sem ele.

**Se sobrar tempo:** acrescente antes do "Obrigado":

> "E tem uma coisa que esse projeto me ensinou: a restrição mais importante
> não estava no documento de requisitos. Estava no lugar onde o aluno ia usar
> o aplicativo."

## Perguntas prováveis

| Pergunta | Resposta curta |
|---|---|
| "De onde vêm esses números?" | "Da análise estatística da base do projeto, com dados fictícios simulados a partir do modelo real." |
| "E a LGPD?" | "Todos os dados da demonstração são fictícios. O público inclui adolescentes, então uso real exigiria consentimento dos responsáveis — está mapeado como próximo passo." |
| "O que falta?" | "Banco gerenciado no lugar do SQLite, notificação push de verdade e publicação na Play Store." |
| "Quem usa o painel?" | "A equipe da ONG. É por lá que o encontro é criado, o QR Code é gerado e o certificado é emitido." |

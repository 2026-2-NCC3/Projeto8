# Pitch — 4 minutos

**Projeto:** Próxima Etapa — App do Aluno
**Critérios avaliados:** criatividade, impacto social, tempo de apresentação e
embasamento dos resultados.

---

## Roteiro cronometrado

> Marcas de tempo acumuladas. Fale em ritmo normal — o texto abaixo tem cerca
> de 480 palavras, que é o que cabe confortavelmente em 4 minutos. **Ensaie
> com cronômetro**: tempo é critério de nota.

---

### 0:00 – 0:35 · O problema (com uma pessoa, não com um gráfico)

> "A Próxima Etapa é uma ONG que conecta jovens de escolas públicas de São
> Paulo ao ensino superior, com cursos, palestras e orientação profissional.
>
> Imagina a Ana, 17 anos, 3º ano do ensino médio. Ela se inscreve num curso de
> redação para o ENEM. Para receber o certificado — aquele que vai no
> currículo e na inscrição de bolsa — ela precisa de 75% de presença.
>
> Hoje essa presença é controlada em lista de papel. E é aí que o problema
> começa."

*(Pausa de um segundo antes da virada.)*

---

### 0:35 – 1:10 · Por que isso importa (embasamento em dados)

> "A gente analisou os dados do projeto e encontrou três coisas.
>
> Primeira: metade dos alunos — **49,8%** — fica abaixo dos 75% de presença.
> Metade. Esses alunos não recebem certificado.
>
> Segunda: a frequência é a variável mais instável de todas. **Coeficiente de
> variação de 49%**. Isso significa que não existe 'o aluno médio': existe
> quem vai a tudo e quem some.
>
> Terceira: a chance de um aluno faltar a um encontro qualquer é de **38,7%**.
>
> Ou seja: a ONG está perdendo alunos no meio do caminho e só descobre isso
> quando o curso acabou e o certificado não saiu."

---

### 1:10 – 1:35 · A solução, em uma frase

> "A gente construiu um sistema completo para a jornada do aluno: um
> **aplicativo Android**, uma **API REST**, um **banco de dados** e um
> **painel web** para a equipe da ONG.
>
> O aluno vê cursos, agenda, certificados e recebe avisos. A equipe cria
> encontros, acompanha frequência e emite certificados. E a presença é
> registrada em dois segundos, por QR Code."

---

### 1:35 – 3:05 · Demonstração ao vivo (o coração do pitch)

> "Deixa eu mostrar o que torna isso diferente de um app comum."

**[PAINEL WEB]** — *mostre a frequência média em destaque*
> "Este é o painel da ONG. Este número aqui é frequência média — é com ele que
> a organização presta contas a quem financia o projeto."

**[CRIAR ENCONTRO E PROJETAR O QR]**
> "Crio o encontro de hoje. O sistema gera um QR Code único, que a equipe
> projeta na sala."

**[APP — ATIVE O MODO AVIÃO ANTES]** — *mostre o ícone de avião na tela*
> "Agora o detalhe que importa. Esses encontros acontecem em escolas e espaços
> comunitários, onde o sinal cai. Então eu vou colocar o celular **em modo
> avião** — sem internet nenhuma."

*(Escaneie o QR Code.)*

> "Escaneei. E olha: **a presença foi confirmada na hora**, mesmo offline. O
> app avisa que está aguardando conexão.
>
> Sem isso, essa presença simplesmente se perdia — e presença perdida é
> certificado perdido."

**[DESLIGUE O MODO AVIÃO, VOLTE À TELA INICIAL]**
> "Volta a internet… e a fila sobe sozinha. O aviso sumiu."

**[PAINEL WEB — ABRA A CHAMADA]**
> "No painel, a Ana já aparece como presente. E repare no horário: é o horário
> em que ela escaneou, não o horário em que a internet voltou. O sistema
> preserva o momento real do check-in."

**[EMITA O CERTIFICADO]**
> "E quando ela bate os 75%, a equipe emite o certificado aqui — e ele chega
> como notificação no celular dela."

---

### 3:05 – 3:40 · Por que as decisões foram essas

> "Duas decisões técnicas que a gente tomou pensando nesse público.
>
> A primeira: **o app funciona offline por padrão**. Nenhuma tela depende da
> internet para abrir — todas leem do banco local do celular, e a rede só
> atualiza isso por baixo.
>
> A segunda: cada check-in carrega um identificador único gerado no celular.
> Se a resposta do servidor se perde e o app tenta de novo, o servidor
> reconhece e **não registra a presença duas vezes**. Numa rede instável, sem
> isso, a correção viraria um problema novo."

---

### 3:40 – 4:00 · Fechamento

> "O sistema está no ar, com os quatro componentes integrados, 45 testes
> automatizados passando e o código aberto no GitHub.
>
> E o que ele resolve, no fim, é simples: garantir que o esforço de um aluno
> que foi ao encontro **não se perca por causa de um sinal de celular**.
>
> Obrigado."

---

## Checklist antes de apresentar

**Até 10 minutos antes**
- [ ] Abrir `https://proxima-etapa-api.onrender.com/api/health` para **acordar
      o servidor** (o plano gratuito hiberna e leva até 50 segundos).
- [ ] Abrir o painel web já **logado**, na tela do painel.
- [ ] Abrir o app já **logado**, na tela inicial.
- [ ] Criar o encontro de demonstração **antes**, e deixar a aba do QR Code
      pronta em segundo plano.
- [ ] Testar o modo avião uma vez, para não descobrir um problema ao vivo.
- [ ] Celular com bateria e **brilho no máximo** (a câmera lê melhor a tela
      projetada).

**Plano B se a internet da faculdade falhar**
- O app abre e navega normalmente offline — isso é parte do argumento, não um
  problema. Mostre o modo offline e diga que a sincronização sobe depois.
- Tenha um **vídeo de 40 segundos** gravado do fluxo completo, no celular.

**Plano C se o QR Code não escanear na projeção**
- Tenha o QR Code aberto também na tela do notebook (perto, sem reflexo) ou
  impresso em papel.

---

## Ajustes de tempo

| Situação | O que cortar |
|---|---|
| Está longo | A seção 3:05–3:40 (decisões técnicas). O impacto já foi mostrado na demo. |
| Está curto | Acrescente, no fechamento: "a mesma estrutura serve para qualquer ONG ou cursinho que precise comprovar frequência — curso, encontro, presença e certificado são genéricos." |
| Perguntarem sobre LGPD | "Todos os dados da demonstração são fictícios, gerados por script. O público inclui adolescentes, então uso real exigiria consentimento dos responsáveis — está mapeado como próximo passo." |
| Perguntarem o que falta | "Banco gerenciado em vez de SQLite, push de verdade com Firebase, e publicação na Play Store." |

---

## Números para ter na ponta da língua

| Número | O que é |
|---|---|
| **49,8%** | dos alunos ficam abaixo de 75% de frequência |
| **49%** | coeficiente de variação da frequência (dispersão alta) |
| **38,7%** | probabilidade de ausência em um encontro |
| **77,8%** | probabilidade de conclusão nas turmas já encerradas |
| **45** | testes automatizados passando (11 da Entrega 1 + 34 da Entrega 2) |
| **4** | componentes integrados: app, API, banco e painel web |

> Os percentuais vêm da análise estatística da base de dados do projeto
> (simulada a partir do modelo real, com dados fictícios). Se perguntarem a
> origem, diga isso — é mais forte do que parecer um número inventado.

# Módulo Web Administrativo — Próxima Etapa

Painel que a equipe da ONG usa para **alimentar o aplicativo Android dos alunos**.
Tudo o que é criado aqui aparece no app na sincronização seguinte: cursos,
encontros, QR Code de presença, comunicados e certificados.

Atende ao item do documento do projeto que pede "um módulo web administrativo
e/ou uma camada de integração que represente as funções necessárias para
alimentar o aplicativo".

## Telas

| Tela | Endereço | O que faz |
|---|---|---|
| Login | `/` | Entrada da equipe, com as credenciais de demonstração à vista |
| Painel | `#/painel` | Frequência média do programa com a meta de 75%, contadores, ocupação dos cursos e próximos encontros |
| Cursos | `#/cursos` | Lista, cria, edita e desativa cursos |
| Curso | `#/cursos/:id` | Ficha do curso e seus encontros |
| QR Code | `#/encontros/:id/qrcode` | Tela de projeção para o telão do encontro, com opção de imprimir |
| Chamada | `#/encontros/:id/chamada` | Quem veio e quem faltou, com percentual da turma |
| Alunos | `#/alunos` | Busca e lista |
| Ficha do aluno | `#/alunos/:id` | Frequência por curso, com destaque abaixo de 75% |
| Comunicados | `#/comunicados` | Envia para todos ou para um curso; histórico com alcance e leitura |
| Certificados | `#/certificados` | Elegíveis por frequência mínima, com emissão |
| Dúvidas | `#/chat` | Conversas dos alunos e resposta da equipe |

## Como rodar

O painel é servido pelo próprio backend, na raiz do domínio
(`express.static`). Não há build, npm nem dependência externa — é HTML, CSS e
JavaScript puro (ES modules), e funciona mesmo sem internet.

```bash
cd "../Backend"
npm install
node db/seed.js      # só na primeira vez, ou para repopular o banco
node server.js
```

Abra **http://localhost:3000/** (ou a porta definida em `PORT`).

## Acesso de demonstração

```
admin@proximaetapa.org.br
123456
```

Dados fictícios, criados para a apresentação. O botão "Preencher
automaticamente" na tela de login já coloca esses valores no formulário.

## Organização dos arquivos

```
ModuloWeb/
├── index.html          estrutura das telas (login, painel, projeção)
├── css/estilo.css      identidade visual e layout
└── js/
    ├── app.js          roteamento por hash, login e montagem das telas
    ├── api.js          chamadas à API, token e tratamento de 401
    ├── ui.js           elementos, formatação em pt-BR, diálogos e avisos
    └── views/          uma tela por arquivo
        ├── painel.js       cursos.js      encontros.js
        ├── alunos.js       comunicados.js certificados.js
        └── chat.js
```

## Decisões que valem registro

- **Identidade do app.** Verde `#1E6F5C`, verde escuro `#154F42` e âmbar
  `#F2A93B`, os mesmos do aplicativo Android. O âmbar aparece só em duas
  situações: o que está acontecendo agora (QR Code, encontro lotado) e a meta
  de 75%.
- **Frequência média em destaque.** É o indicador que a ONG usa para prestar
  contas a financiadores, então vem acompanhado da meta e de uma frase dizendo
  se está dentro ou fora dela — número sozinho não informa.
- **O QR Code precisa de token.** A rota `qrcode.svg` exige o cabeçalho
  `Authorization`, então o SVG é buscado com `fetch` e injetado no DOM; um
  `<img src>` não conseguiria enviar o token.
- **Curso não se apaga.** O botão desativa, preservando presenças e
  certificados já emitidos.
- **Sessão de um dia.** Qualquer resposta 401 limpa o token e devolve ao
  login, com aviso explicando o motivo.
- **Acessibilidade.** Rótulo em todo campo, foco visível, navegação por
  teclado, contraste conferido, confirmação antes de ação destrutiva e estados
  de carregando/vazio/erro em todas as listas.

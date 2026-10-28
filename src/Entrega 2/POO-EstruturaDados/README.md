# POO e Estrutura de Dados — Próxima Etapa (Entrega 2)

Módulo da UC **Programação Orientada a Objetos e Estrutura de Dados** do
Projeto Interdisciplinar **"Próxima Etapa — App do Aluno"**.

Java puro, **sem Android e sem nenhuma dependência externa**: compila com
`javac` e roda com `java`. As classes espelham o modelo real do projeto —
o banco em [`../Backend/db/schema.sql`](../Backend/db/schema.sql) é a fonte da
verdade dos atributos, dos relacionamentos e das restrições `UNIQUE`.

Diagrama de classes completo:
[`documentos/Entrega 2/POO e Estrutura de Dados/diagrama-classes-completo.md`](../../../documentos/Entrega%202/POO%20e%20Estrutura%20de%20Dados/diagrama-classes-completo.md)

---

## Como compilar e rodar

```bash
cd "src/Entrega 2/POO-EstruturaDados"
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -cp out br.org.proximaetapa.poo.app.Main
```

> Se os acentos saírem como `?`, é o locale do terminal, não o código:
> rode com `LANG=C.UTF-8` antes do comando `java`, ou abra o projeto em uma
> IDE (IntelliJ/Eclipse), que já usa UTF-8.

A execução imprime 13 seções numeradas demonstrando cadastrar, visualizar,
localizar por id, buscar por filtro, editar, remover, ordenar, a agenda
cronológica, a recusa de presença duplicada, a fila de sincronização offline
sendo drenada em ordem e a emissão de certificado por frequência.

---

## Estrutura de pastas

```
src/br/org/proximaetapa/poo/
  modelo/        13 classes de domínio + interface Identificavel + superclasse ItemFeed
  repositorio/   estruturas de dados (ArrayList, HashMap, TreeMap, HashSet, Queue, Comparator)
  servico/       regras de negócio que cruzam entidades
  app/Main.java  demonstração executável
```

---

## Decisão de estrutura de dados (destaque)

> **Decisão:** indexar cada entidade em **dois** lugares ao mesmo tempo — uma
> `ArrayList` que guarda a ordem de inserção e um `HashMap` que guarda o id —
> e estender essa ideia a um **segundo índice por QR Code** no
> `RepositorioAtividade`.

**O problema concreto.** O check-in de presença é a operação mais frequente e
mais sensível a atraso do app: a turma inteira passa o celular no leitor na
porta da oficina, ao mesmo tempo. O leitor devolve apenas a **string do QR
Code** (`POST /presencas/checkin` recebe `qr_code`), nunca o id da atividade.
Com uma lista simples, cada leitura exigiria percorrer todas as atividades de
todos os cursos comparando strings: **O(n)**, piorando a cada semestre em que a
ONG cadastra novos encontros.

**A solução.** O `RepositorioAtividade` herda o par `ArrayList + HashMap` do
repositório genérico e acrescenta um `HashMap<String, Atividade>` com o QR Code
como chave. O check-in passa a ser resolvido por **três consultas O(1) em
sequência**, nenhuma delas dependente do tamanho do histórico:

| Verificação | Estrutura | Custo |
|---|---|---|
| O QR Code existe? | `HashMap<String, Atividade>` (`RepositorioAtividade`) | O(1) |
| O aluno está inscrito na turma? | `HashSet<String>` de `alunoId:cursoId` (`ServicoInscricao`) | O(1) |
| A presença já foi registrada? | `HashSet<Presenca.Chave>` (`ControleDePresenca`) | O(1) |

**O ganho.** O tempo do check-in deixa de depender do volume de dados:
com 7 encontros ou com 7.000, a resposta é a mesma. O custo pago é memória
(as referências aparecem em mais de uma estrutura) — troca vantajosa, porque o
protótipo tem muito mais leitura do que escrita. Os três índices são a
tradução em memória das restrições que o banco já garante
(`UNIQUE(qr_code)`, `UNIQUE(aluno_id, curso_id)`,
`UNIQUE(aluno_id, atividade_id)`): a mesma regra vale no app offline e no
servidor.

---

## Estruturas de dados empregadas

| Estrutura | Onde | Por que no domínio | Complexidade |
|---|---|---|---|
| `ArrayList<T>` | `RepositorioEmMemoria` | Preserva a ordem de cadastro, que é a ordem exibida nas listagens | inserir O(1) amortizado; percorrer/filtrar O(n) |
| `HashMap<Integer,T>` | `RepositorioEmMemoria` | Índice por id: abrir um curso ou um aluno direto pela tela, sem varrer a lista | localizar/editar O(1) médio |
| `HashMap<String,Atividade>` | `RepositorioAtividade` | Check-in: o leitor só tem a string do QR Code em mãos | O(1) médio |
| `TreeMap<LocalDateTime,List<Atividade>>` | `AgendaDoAluno` | A agenda sai sempre em ordem cronológica e responde a perguntas de intervalo ("próximos encontros", "o que tenho nesta semana") | inserir O(log n); `tailMap`/`headMap`/`subMap` O(log n) + O(k); percorrer em ordem O(n) |
| `TreeMap<Integer,Pergunta>` | `TestePerfil` | O questionário tem sequência pedagógica fixa (`ORDER BY ordem` no backend); o mapa ordena na inserção, então exibir não precisa ordenar | O(log n) |
| `HashSet<Presenca.Chave>` | `ControleDePresenca` | Espelha `UNIQUE(aluno_id, atividade_id)`: rejeita a segunda leitura do mesmo QR Code pelo mesmo aluno | `contains`/`add` O(1) médio |
| `HashSet<String>` | `ServicoInscricao`, `ServicoCertificado` | Espelha `UNIQUE(aluno_id, curso_id)` nas tabelas `inscricoes` e `certificados` | O(1) médio |
| `Queue` / `LinkedList` | `FilaDeSincronizacao` | Check-ins feitos sem internet precisam subir ao servidor **na mesma ordem em que o aluno passou o QR Code** — o `registrado_em` é o comprovante do horário de entrada | `offer`/`poll`/`peek` O(1) |
| `LinkedHashMap<Integer,Alternativa>` | `Pergunta` | O app responde `{pergunta_id: alternativa_id}`; corrigir exige achar a alternativa pelo id sem perder a ordem de exibição | O(1) médio |
| `EnumMap<Perfil,Integer>` | `TestePerfil.apurar` | Contagem por perfil: chaves de um `enum` fechado, implementado como vetor pelo ordinal | O(1), menor consumo que `HashMap` |
| `Comparator<T>` | `Comparadores` | Ranking por pontos, cursos por data ou por vagas, feed por recência — sem reescrever o repositório | ordenar O(n log n) |

### Por que `TreeMap` e não `ArrayList` ordenado na agenda

Manter uma `ArrayList` ordenada exigiria reordená-la a cada inserção
(O(n log n)) ou ordenar na hora de exibir, e a consulta "quais encontros ainda
vêm a partir de agora?" varreria a lista toda (O(n)). O `TreeMap` é uma árvore
balanceada: a inserção já coloca o encontro no lugar cronológico certo
(O(log n)), iterar devolve tudo em ordem sem ordenar nada, e `tailMap`,
`headMap` e `subMap` posicionam na árvore em O(log n) e percorrem **apenas a
faixa pedida**. O valor do mapa é uma `List<Atividade>` porque dois cursos
podem ter encontros no mesmo dia e horário — conflito de agenda que o app
precisa mostrar, não esconder.

---

## Princípios de POO aplicados

### Encapsulamento

| Onde | O que protege |
|---|---|
| `Curso.vagasDisponiveis` | Campo privado **sem setter**. Só muda por `ocuparVaga()` / `liberarVaga()`, que mantêm o invariante `0 <= disponíveis <= totais`. Sem isso o app aceitaria inscrição em curso lotado. |
| `Aluno.pontos` / `nivel` | Só crescem por `adicionarPontos(int)`, que recusa valor não positivo e **recalcula o nível** junto — os dois campos nunca ficam inconsistentes. |
| `Aluno.id`, `email`, `criadoEm` | Sem setter: id é identidade, e-mail é `UNIQUE` no banco, data de criação é histórica. |
| `Inscricao.status` | Avança só por `iniciar()`, `concluir()` e `desistir()`, que barram transições inválidas (não se desiste de curso concluído). |
| `Notificacao.lida` | Só vira `true` por `marcarComoLida()`; nunca volta para não lida. |
| `Certificado` | Construtor **privado**: só se cria certificado pela fábrica `emitir(...)`, que recusa frequência abaixo de 75%. Não existe certificado inválido em memória. |
| `Card` | Projeção imutável do aluno que **não** carrega e-mail nem hash de senha — é uma tela que pode ser mostrada a terceiros. |
| `ResultadoPerfil` | Imutável; mapas devolvidos como `unmodifiableMap`. É registro histórico. |

### Herança

**`ItemFeed`** (`modelo/ItemFeed.java`) é a superclasse abstrata de
**`Mensagem`** e **`Notificacao`**. As tabelas `mensagens` e `notificacoes`
compartilham exatamente a mesma tríade *título + corpo + data de criação*, e as
duas aparecem no mesmo feed da tela inicial. Na superclasse ficam, **uma única
vez**, os três atributos, seus getters, o `equals`/`hashCode` e a formatação da
linha do feed; cada subclasse declara só o que é seu — `Mensagem` o
destinatário (`TODOS` ou `CURSO`), `Notificacao` o dono, o tipo e o estado
lida/não lida.

Há uma segunda herança, aplicada à estrutura de dados:
**`RepositorioAtividade extends RepositorioEmMemoria<Atividade>`** reaproveita
toda a mecânica de lista + índice por id e sobrescreve `cadastrar` e `remover`
(chamando `super`) para manter sincronizado o índice extra por QR Code.

### Polimorfismo

- **Sobrescrita:** `rotuloTipo()` é abstrato em `ItemFeed`; `Mensagem` devolve
  `COMUNICADO`/`CURSO` e `Notificacao` devolve `AVISO/<tipo>`. `Notificacao`
  ainda sobrescreve `linhaDoFeed()` chamando `super.linhaDoFeed()` e
  acrescentando o marcador de não lida. Em `ServicoFeed.feedDoAluno`, o feed é
  uma única `List<ItemFeed>` ordenada por **um** comparador e impressa por
  **uma** chamada `item.linhaDoFeed()` — sem nenhum `if (x instanceof ...)`.
  Incluir um novo tipo de item no feed é criar outra subclasse; o serviço não
  muda.
- **Interface:** `RepositorioEmMemoria<T extends Identificavel>` chama
  `item.getId()` sem saber se T é Aluno, Curso, Certificado ou Mensagem.
- **Paramétrico (genéricos):** o mesmo repositório atende as 13 entidades.
- **Comparadores:** `listarOrdenado(Comparator<T>)` troca o critério de
  ordenação sem tocar no repositório.

### Abstração

- **`Identificavel`** reduz toda entidade ao que o repositório precisa saber:
  ela tem um id. (Na Entrega 1 era preciso passar uma
  `Function<T,Integer>` no construtor; agora o contrato resolve isso em tempo
  de compilação.)
- **`Queue`** na declaração e `LinkedList` só na criação: trocar por
  `ArrayDeque` não muda uma linha de `FilaDeSincronizacao`.
- **`FilaDeSincronizacao.drenar(Function<...>)`** não sabe o que é "registrar
  presença"; só garante a ordem. Quem conhece a regra é o `ServicoPresenca`.
- **`Resultado`** uniformiza o retorno dos serviços: as recusas do domínio
  (curso lotado, presença duplicada, frequência insuficiente) são caminhos
  previstos, não exceções.

---

## Regras de negócio no modelo

| Regra | Onde está |
|---|---|
| Presença não pode duplicar no mesmo encontro | `ControleDePresenca.registrar` + `Presenca.Chave` (HashSet) |
| Curso não aceita inscrição sem vaga | `Curso.temVaga()` / `Curso.ocuparVaga()`, aplicado por `ServicoInscricao.inscrever` |
| Um aluno não se inscreve duas vezes no mesmo curso | `HashSet` de `Inscricao.chaveUnica()` em `ServicoInscricao` |
| Certificado só com 75% de frequência | `Certificado.atendeCriterioDeFrequencia` + fábrica `Certificado.emitir` |
| Um certificado por aluno por curso | `HashSet` de `Certificado.chaveUnica()` em `ServicoCertificado` |
| Check-in exige inscrição ativa na turma | `ServicoPresenca.registrarCheckIn` |
| Transições de status da inscrição | `Inscricao.iniciar/concluir/desistir` |

---

## O que mudou em relação à Entrega 1

| Entrega 1 | Entrega 2 |
|---|---|
| 5 classes de modelo | 13 classes de domínio + `Identificavel` + `ItemFeed` |
| `RepositorioEmMemoria<T>` com `Function<T,Integer>` no construtor | `RepositorioEmMemoria<T extends Identificavel>`, sem extrator |
| ArrayList + HashMap | \+ TreeMap, HashSet, Queue/LinkedList, LinkedHashMap, EnumMap, Comparator |
| Atributos mínimos | Atributos espelhando `schema.sql`, com `enum` no lugar dos `TEXT` com `CHECK` |
| Sem camada de serviço | `servico/` com as regras que cruzam entidades |
| Sem herança | `ItemFeed` → `Mensagem`/`Notificacao`; `RepositorioEmMemoria` → `RepositorioAtividade` |

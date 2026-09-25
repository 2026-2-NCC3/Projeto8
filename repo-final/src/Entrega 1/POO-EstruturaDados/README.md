# POO e Estrutura de Dados — Próxima Etapa

Java puro (sem dependências externas), compila e roda com `javac`/`java`
diretamente — não depende do Android Studio.

## Classes do modelo (mínimo exigido na Entrega 1)

`Aluno`, `Curso`, `Atividade`, `Inscrição`, `Presença` — em
`src/br/org/proximaetapa/poo/modelo/`.

## Estrutura de dados

`RepositorioEmMemoria<T>` (`src/br/org/proximaetapa/poo/repositorio/`) é um
repositório genérico usado por todas as entidades, combinando:

- **`ArrayList<T>`** — mantém a ordem de inserção; usado para listar, filtrar
  e ordenar.
- **`HashMap<Integer, T>`** — índice por id, usado para localizar, editar e
  remover em tempo médio O(1), em vez de varrer a lista inteira.

Isso evidencia a decisão de estrutura de dados pedida na Entrega 2 (combinar
lista + índice hash para acesso rápido por id sem perder a ordem de
inserção).

## Operações implementadas

Visualizar, cadastrar, editar, localizar (por id e por filtro) e remover —
ver `Main.java` para uma demonstração completa, incluindo a regra de negócio
que evita presença duplicada no mesmo encontro.

## Como compilar e rodar

```bash
cd poo-estrutura-dados
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -cp out br.org.proximaetapa.poo.app.Main
```

> Se os acentos aparecerem como `?` no terminal, é só uma questão de
> locale do terminal (não do código) — rode com `LANG=C.utf8` antes do
> comando `java`, ou apenas abra o projeto em uma IDE (IntelliJ/Eclipse),
> que já usa UTF-8 por padrão.

## Diagrama de classes

Ver `docs/diagrama-classes.md` (diagrama em Mermaid, renderiza automaticamente
no GitHub) na raiz do repositório.

# Diagrama de Classes — Próxima Etapa (Entrega 1)

Contempla o mínimo exigido: `Aluno`, `Curso`, `Atividade`, `Inscrição`,
`Presença`. As demais classes (Certificado, Card, TestePerfil, Pergunta,
Alternativa, ResultadoPerfil, Mensagem, Notificação) entram na Entrega 2,
conforme o cronograma do projeto — já estão modeladas como tabelas no
banco (ver `backend/db/schema.sql`) e serão promovidas a classes Java no
próximo incremento.

```mermaid
classDiagram
    class Aluno {
        -int id
        -String nome
        -String email
        -String escola
        -int pontos
        +getId() int
        +adicionarPontos(int) void
    }

    class Curso {
        -int id
        -String titulo
        -String categoria
        -int cargaHoraria
        -int vagasTotais
        -int vagasDisponiveis
        +ocuparVaga() boolean
    }

    class Atividade {
        -int id
        -int cursoId
        -String titulo
        -LocalDate data
    }

    class Inscricao {
        -int id
        -int alunoId
        -int cursoId
        -Status status
    }

    class Presenca {
        -int id
        -int alunoId
        -int atividadeId
        -LocalDateTime registradaEm
        +chaveUnica() String
    }

    class RepositorioEmMemoria~T~ {
        -ArrayList~T~ itens
        -HashMap~Integer,T~ indicePorId
        +cadastrar(T) T
        +localizarPorId(int) Optional~T~
        +localizar(Predicate) List~T~
        +editar(int, T) boolean
        +remover(int) boolean
        +listarOrdenado(Comparator) List~T~
    }

    Aluno "1" --> "0..*" Inscricao : possui
    Curso "1" --> "0..*" Inscricao : recebe
    Curso "1" --> "0..*" Atividade : programa
    Aluno "1" --> "0..*" Presenca : registra
    Atividade "1" --> "0..*" Presenca : gera
    RepositorioEmMemoria ..> Aluno : gerencia
    RepositorioEmMemoria ..> Curso : gerencia
    RepositorioEmMemoria ..> Atividade : gerencia
    RepositorioEmMemoria ..> Inscricao : gerencia
    RepositorioEmMemoria ..> Presenca : gerencia
```

## Como visualizar

- No GitHub: este arquivo já renderiza o diagrama automaticamente ao abrir
  o `.md` no navegador.
- Fora do GitHub: cole o bloco `mermaid` em <https://mermaid.live> para ver
  a imagem.

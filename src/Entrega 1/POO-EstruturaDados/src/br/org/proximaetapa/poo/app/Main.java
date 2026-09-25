package br.org.proximaetapa.poo.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import br.org.proximaetapa.poo.modelo.Aluno;
import br.org.proximaetapa.poo.modelo.Atividade;
import br.org.proximaetapa.poo.modelo.Curso;
import br.org.proximaetapa.poo.modelo.Inscricao;
import br.org.proximaetapa.poo.modelo.Presenca;
import br.org.proximaetapa.poo.repositorio.RepositorioEmMemoria;

/**
 * Demonstração do modelo de domínio (Aluno, Curso, Atividade, Inscrição,
 * Presença) com operações de visualizar, cadastrar, editar, localizar e
 * remover usando as estruturas de dados do RepositorioEmMemoria
 * (ArrayList + HashMap), conforme exigido na Entrega 1 da UC de POO e
 * Estrutura de Dados.
 *
 * Compilar e rodar (sem dependências externas):
 *   javac -d out $(find src -name "*.java")
 *   java -cp out br.org.proximaetapa.poo.app.Main
 */
public class Main {

    public static void main(String[] args) {
        RepositorioEmMemoria<Aluno> alunos = new RepositorioEmMemoria<>(Aluno::getId);
        RepositorioEmMemoria<Curso> cursos = new RepositorioEmMemoria<>(Curso::getId);
        RepositorioEmMemoria<Atividade> atividades = new RepositorioEmMemoria<>(Atividade::getId);
        RepositorioEmMemoria<Inscricao> inscricoes = new RepositorioEmMemoria<>(Inscricao::getId);
        RepositorioEmMemoria<Presenca> presencas = new RepositorioEmMemoria<>(Presenca::getId);

        System.out.println("=== 1. CADASTRAR ===");
        Aluno ana = alunos.cadastrar(new Aluno(1, "Ana Souza", "ana.souza@aluno.proximaetapa.org.br", "EE Prof. João XXIII"));
        Aluno bruno = alunos.cadastrar(new Aluno(2, "Bruno Lima", "bruno.lima@aluno.proximaetapa.org.br", "EE Dom Pedro II"));
        Aluno carla = alunos.cadastrar(new Aluno(3, "Carla Mendes", "carla.mendes@aluno.proximaetapa.org.br", "EE Castro Alves"));
        System.out.println("Alunos cadastrados: " + alunos.total());

        Curso progIntro = cursos.cadastrar(new Curso(1, "Introdução à Programação", "Tecnologia", 20, 25));
        Curso redacao = cursos.cadastrar(new Curso(2, "Oficina de Redação", "Educação", 12, 30));
        System.out.println("Cursos cadastrados: " + cursos.total());

        Atividade encontro1 = atividades.cadastrar(new Atividade(1, progIntro.getId(), "Encontro 1 - Lógica", LocalDate.of(2026, 9, 4)));
        Atividade encontro2 = atividades.cadastrar(new Atividade(2, progIntro.getId(), "Encontro 2 - Variáveis", LocalDate.of(2026, 9, 11)));
        atividades.cadastrar(new Atividade(3, redacao.getId(), "Encontro 1 - Estrutura textual", LocalDate.of(2026, 9, 5)));

        inscricoes.cadastrar(new Inscricao(1, ana.getId(), progIntro.getId()));
        inscricoes.cadastrar(new Inscricao(2, bruno.getId(), progIntro.getId()));
        inscricoes.cadastrar(new Inscricao(3, carla.getId(), redacao.getId()));
        progIntro.ocuparVaga();
        progIntro.ocuparVaga();
        redacao.ocuparVaga();

        presencas.cadastrar(new Presenca(1, ana.getId(), encontro1.getId(), LocalDateTime.of(2026, 9, 4, 18, 5)));
        presencas.cadastrar(new Presenca(2, bruno.getId(), encontro1.getId(), LocalDateTime.of(2026, 9, 4, 18, 7)));

        System.out.println("\n=== 2. EVITANDO PRESENÇA DUPLICADA (regra de negócio) ===");
        try {
            // Ana tentando registrar presença de novo no mesmo encontro
            Presenca duplicada = new Presenca(3, ana.getId(), encontro1.getId(), LocalDateTime.now());
            boolean jaExiste = presencas.localizar(p -> p.chaveUnica().equals(duplicada.chaveUnica())).size() > 0;
            if (jaExiste) {
                System.out.println("Bloqueado: " + ana.getNome() + " já registrou presença nesse encontro.");
            } else {
                presencas.cadastrar(duplicada);
            }
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }

        System.out.println("\n=== 3. LOCALIZAR POR ID (HashMap, O(1)) ===");
        alunos.localizarPorId(2).ifPresent(a -> System.out.println("Encontrado: " + a));

        System.out.println("\n=== 4. LOCALIZAR COM FILTRO (ex.: alunos da mesma escola) ===");
        List<Aluno> mesmaEscola = alunos.localizar(a -> a.getEscola().equals("EE Dom Pedro II"));
        mesmaEscola.forEach(System.out::println);

        System.out.println("\n=== 5. EDITAR ===");
        ana.adicionarPontos(150);
        alunos.editar(ana.getId(), ana);
        System.out.println("Após edição: " + alunos.localizarPorId(1).get());

        System.out.println("\n=== 6. ORDENAR (Comparator — mecanismo de organização de dados) ===");
        List<Aluno> porPontosDesc = alunos.listarOrdenado(Comparator.comparingInt(Aluno::getPontos).reversed());
        porPontosDesc.forEach(System.out::println);

        System.out.println("\n=== 7. REMOVER ===");
        boolean removido = inscricoes.remover(3);
        System.out.println("Inscrição#3 removida? " + removido);
        System.out.println("Inscrições restantes: " + inscricoes.total());

        System.out.println("\n=== 8. VISUALIZAR TODOS ===");
        System.out.println("-- Alunos --");
        alunos.listarTodos().forEach(System.out::println);
        System.out.println("-- Cursos --");
        cursos.listarTodos().forEach(System.out::println);
        System.out.println("-- Atividades --");
        atividades.listarTodos().forEach(System.out::println);
        System.out.println("-- Presenças --");
        presencas.listarTodos().forEach(System.out::println);

        System.out.println("\nDemonstração concluída com sucesso.");
    }
}

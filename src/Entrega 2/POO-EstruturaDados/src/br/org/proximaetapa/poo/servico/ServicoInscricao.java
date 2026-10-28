package br.org.proximaetapa.poo.servico;

import br.org.proximaetapa.poo.modelo.Aluno;
import br.org.proximaetapa.poo.modelo.Atividade;
import br.org.proximaetapa.poo.modelo.Curso;
import br.org.proximaetapa.poo.modelo.Inscricao;
import br.org.proximaetapa.poo.repositorio.AgendaDoAluno;
import br.org.proximaetapa.poo.repositorio.RepositorioAtividade;
import br.org.proximaetapa.poo.repositorio.RepositorioEmMemoria;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Regras de inscrição em curso (RFM03/RFM04) — cruza Aluno, Curso, Inscrição e
 * a agenda de encontros.
 *
 * <h2>Estruturas de dados</h2>
 * <ul>
 *   <li>{@code HashSet<String>} com a chave {@code alunoId:cursoId}: espelha o
 *       {@code UNIQUE(aluno_id, curso_id)} da tabela {@code inscricoes} e
 *       responde "este aluno já está inscrito?" em O(1), em vez de varrer todas
 *       as inscrições (O(n)). É a mesma consulta que o backend faz antes do
 *       check-in para saber se o aluno pertence à turma.</li>
 *   <li>{@code HashMap<Integer, AgendaDoAluno>}: uma agenda (TreeMap) por
 *       aluno, localizada pelo id em O(1). Ao se inscrever, os encontros do
 *       curso são copiados para a agenda do aluno já em ordem cronológica.</li>
 * </ul>
 */
public class ServicoInscricao {

    private final RepositorioEmMemoria<Aluno> alunos;
    private final RepositorioEmMemoria<Curso> cursos;
    private final RepositorioEmMemoria<Inscricao> inscricoes;
    private final RepositorioAtividade atividades;

    /** Índice de unicidade aluno+curso — O(1). */
    private final Set<String> chavesDeInscricao = new HashSet<>();

    /** Uma agenda cronológica por aluno. */
    private final Map<Integer, AgendaDoAluno> agendas = new HashMap<>();

    public ServicoInscricao(RepositorioEmMemoria<Aluno> alunos,
                            RepositorioEmMemoria<Curso> cursos,
                            RepositorioEmMemoria<Inscricao> inscricoes,
                            RepositorioAtividade atividades) {
        this.alunos = alunos;
        this.cursos = cursos;
        this.inscricoes = inscricoes;
        this.atividades = atividades;
    }

    /**
     * Inscreve o aluno no curso aplicando, em ordem, as regras do projeto:
     * aluno existe, curso existe, aluno ainda não inscrito, curso com vaga.
     * Só depois de tudo validado a vaga é consumida.
     */
    public Resultado inscrever(int alunoId, int cursoId) {
        Optional<Aluno> aluno = alunos.localizarPorId(alunoId);
        if (aluno.isEmpty()) {
            return Resultado.recusa("Aluno " + alunoId + " não encontrado.");
        }
        Optional<Curso> curso = cursos.localizarPorId(cursoId);
        if (curso.isEmpty()) {
            return Resultado.recusa("Curso " + cursoId + " não encontrado.");
        }

        String chave = Inscricao.chaveUnica(alunoId, cursoId);
        if (chavesDeInscricao.contains(chave)) {     // O(1)
            return Resultado.recusa(aluno.get().getNome()
                    + " já possui inscrição em \"" + curso.get().getTitulo() + "\".");
        }

        // REGRA: curso sem vaga (ou inativo) não aceita inscrição.
        if (!curso.get().ocuparVaga()) {
            return Resultado.recusa("Sem vagas em \"" + curso.get().getTitulo()
                    + "\" (" + curso.get().getVagasOcupadas() + "/"
                    + curso.get().getVagasTotais() + " ocupadas).");
        }

        Inscricao inscricao = new Inscricao(inscricoes.proximoId(), alunoId, cursoId);
        inscricoes.cadastrar(inscricao);
        chavesDeInscricao.add(chave);

        // Os encontros do curso entram na agenda do aluno, em ordem cronológica.
        AgendaDoAluno agenda = agendaDo(alunoId);
        for (Atividade atividade : atividades.localizar(a -> a.getCursoId() == cursoId)) {
            agenda.adicionar(atividade);
        }

        return Resultado.sucesso("Inscrição de " + aluno.get().getNome() + " em \""
                + curso.get().getTitulo() + "\" confirmada (restam "
                + curso.get().getVagasDisponiveis() + " vaga(s)).", inscricao);
    }

    /** Desistência: libera a vaga e remove os encontros da agenda do aluno. */
    public Resultado desistir(int alunoId, int cursoId) {
        Optional<Inscricao> inscricao = localizar(alunoId, cursoId);
        if (inscricao.isEmpty()) {
            return Resultado.recusa("Inscrição não encontrada para aluno " + alunoId
                    + " no curso " + cursoId + ".");
        }
        if (!inscricao.get().desistir()) {
            return Resultado.recusa("Curso já concluído — não é possível desistir.");
        }
        cursos.localizarPorId(cursoId).ifPresent(Curso::liberarVaga);
        AgendaDoAluno agenda = agendaDo(alunoId);
        for (Atividade atividade : atividades.localizar(a -> a.getCursoId() == cursoId)) {
            agenda.remover(atividade);
        }
        return Resultado.sucesso("Desistência registrada; vaga devolvida ao curso.",
                inscricao.get());
    }

    /** Consulta O(1) usada pelo check-in: o aluno pertence a esta turma? */
    public boolean estaInscrito(int alunoId, int cursoId) {
        return chavesDeInscricao.contains(Inscricao.chaveUnica(alunoId, cursoId));
    }

    public Optional<Inscricao> localizar(int alunoId, int cursoId) {
        return inscricoes.localizarPrimeiro(
                i -> i.getAlunoId() == alunoId && i.getCursoId() == cursoId);
    }

    public List<Inscricao> inscricoesDoAluno(int alunoId) {
        return inscricoes.localizar(i -> i.getAlunoId() == alunoId);
    }

    public List<Inscricao> inscricoesDoCurso(int cursoId) {
        return inscricoes.localizar(i -> i.getCursoId() == cursoId && i.estaAtiva());
    }

    /** Quantos cursos ativos o aluno tem (dado exibido no Card digital). */
    public int cursosAtivos(int alunoId) {
        return inscricoes.localizar(i -> i.getAlunoId() == alunoId && i.estaAtiva()).size();
    }

    /** Agenda do aluno, criada na primeira vez que é pedida. */
    public AgendaDoAluno agendaDo(int alunoId) {
        return agendas.computeIfAbsent(alunoId, AgendaDoAluno::new);
    }
}

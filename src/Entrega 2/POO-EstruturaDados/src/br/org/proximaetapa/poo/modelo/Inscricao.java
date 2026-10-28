package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Vínculo entre um aluno e um curso (tabela {@code inscricoes}).
 * O banco tem {@code UNIQUE(aluno_id, curso_id)}: um aluno não pode se
 * inscrever duas vezes no mesmo curso. Essa restrição é espelhada em Java pelo
 * {@link #chaveUnica()} guardado em um {@code HashSet} no serviço de inscrição.
 *
 * <p>ENCAPSULAMENTO: o {@code status} não tem setter livre; ele só avança pelos
 * métodos {@link #iniciar()}, {@link #concluir()} e {@link #desistir()}, que
 * impedem transições inválidas (por exemplo, concluir uma inscrição já
 * desistente).</p>
 */
public class Inscricao implements Identificavel {

    public enum Status { INSCRITO, CURSANDO, CONCLUIDO, DESISTENTE }

    private final int id;
    private final int alunoId;
    private final int cursoId;
    private Status status;
    private final LocalDateTime criadoEm;

    public Inscricao(int id, int alunoId, int cursoId) {
        this.id = id;
        this.alunoId = alunoId;
        this.cursoId = cursoId;
        this.status = Status.INSCRITO;
        this.criadoEm = LocalDateTime.now();
    }

    @Override
    public int getId() { return id; }

    public int getAlunoId() { return alunoId; }

    public int getCursoId() { return cursoId; }

    public Status getStatus() { return status; }

    public LocalDateTime getCriadoEm() { return criadoEm; }

    /** Chave que espelha o UNIQUE(aluno_id, curso_id) do banco. */
    public String chaveUnica() {
        return chaveUnica(alunoId, cursoId);
    }

    /** Versão estática: permite checar duplicidade antes de criar o objeto. */
    public static String chaveUnica(int alunoId, int cursoId) {
        return alunoId + ":" + cursoId;
    }

    /** O aluno registrou a primeira presença: a inscrição passa a "cursando". */
    public void iniciar() {
        if (status == Status.INSCRITO) status = Status.CURSANDO;
    }

    /** REGRA: só conclui quem está inscrito ou cursando. */
    public boolean concluir() {
        if (status == Status.DESISTENTE || status == Status.CONCLUIDO) return false;
        status = Status.CONCLUIDO;
        return true;
    }

    /** REGRA: não se desiste de um curso já concluído. */
    public boolean desistir() {
        if (status == Status.CONCLUIDO) return false;
        status = Status.DESISTENTE;
        return true;
    }

    public boolean estaAtiva() {
        return status == Status.INSCRITO || status == Status.CURSANDO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inscricao)) return false;
        return id == ((Inscricao) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Inscricao#%d [aluno=%d | curso=%d | %s]",
                id, alunoId, cursoId, status);
    }
}

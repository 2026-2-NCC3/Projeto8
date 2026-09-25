package br.org.proximaetapa.poo.modelo;

import java.util.Objects;

public class Inscricao {
    public enum Status { INSCRITO, CURSANDO, CONCLUIDO, DESISTENTE }

    private final int id;
    private final int alunoId;
    private final int cursoId;
    private Status status;

    public Inscricao(int id, int alunoId, int cursoId) {
        this.id = id;
        this.alunoId = alunoId;
        this.cursoId = cursoId;
        this.status = Status.INSCRITO;
    }

    public int getId() { return id; }
    public int getAlunoId() { return alunoId; }
    public int getCursoId() { return cursoId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

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
        return String.format("Inscricao#%d [aluno=%d | curso=%d | %s]", id, alunoId, cursoId, status);
    }
}

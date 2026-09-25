package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/** Uma presença confirmada de um aluno em uma atividade (registro por QR Code). */
public class Presenca {
    private final int id;
    private final int alunoId;
    private final int atividadeId;
    private final LocalDateTime registradaEm;

    public Presenca(int id, int alunoId, int atividadeId, LocalDateTime registradaEm) {
        this.id = id;
        this.alunoId = alunoId;
        this.atividadeId = atividadeId;
        this.registradaEm = registradaEm;
    }

    public int getId() { return id; }
    public int getAlunoId() { return alunoId; }
    public int getAtividadeId() { return atividadeId; }
    public LocalDateTime getRegistradaEm() { return registradaEm; }

    /** Chave usada para evitar presença duplicada no mesmo encontro (regra de negócio do projeto). */
    public String chaveUnica() {
        return alunoId + "-" + atividadeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Presenca)) return false;
        return id == ((Presenca) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Presenca#%d [aluno=%d | atividade=%d | %s]", id, alunoId, atividadeId, registradaEm);
    }
}

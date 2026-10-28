package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Presença confirmada de um aluno em uma atividade, registrada pela leitura do
 * QR Code do encontro (tabela {@code presencas}).
 *
 * <p>O banco tem {@code UNIQUE(aluno_id, atividade_id)}. Em memória, essa mesma
 * regra é garantida pelo {@code HashSet<Chave>} do
 * {@code ControleDePresenca}, que rejeita a duplicata em O(1) — sem precisar
 * varrer a lista de presenças.</p>
 */
public class Presenca implements Identificavel {

    /**
     * Par (aluno, atividade) usado como elemento do HashSet de unicidade.
     * Sendo um {@code record}, o Java já gera {@code equals} e {@code hashCode}
     * pelos dois campos — exatamente o que o HashSet precisa para detectar a
     * duplicata.
     */
    public record Chave(int alunoId, int atividadeId) {
        @Override
        public String toString() { return alunoId + ":" + atividadeId; }
    }

    private final int id;
    private final int alunoId;
    private final int atividadeId;
    private final LocalDateTime registradoEm;
    private final boolean sincronizadaOffline;

    public Presenca(int id, int alunoId, int atividadeId, LocalDateTime registradoEm) {
        this(id, alunoId, atividadeId, registradoEm, false);
    }

    public Presenca(int id, int alunoId, int atividadeId,
                    LocalDateTime registradoEm, boolean sincronizadaOffline) {
        this.id = id;
        this.alunoId = alunoId;
        this.atividadeId = atividadeId;
        this.registradoEm = registradoEm == null ? LocalDateTime.now() : registradoEm;
        this.sincronizadaOffline = sincronizadaOffline;
    }

    @Override
    public int getId() { return id; }

    public int getAlunoId() { return alunoId; }

    public int getAtividadeId() { return atividadeId; }

    public LocalDateTime getRegistradoEm() { return registradoEm; }

    /** Indica que o check-in foi feito sem internet e subiu depois pela fila. */
    public boolean isSincronizadaOffline() { return sincronizadaOffline; }

    /** Chave de unicidade que espelha o UNIQUE(aluno_id, atividade_id). */
    public Chave chaveUnica() {
        return new Chave(alunoId, atividadeId);
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
        return String.format("Presenca#%d [aluno=%d | atividade=%d | %s%s]",
                id, alunoId, atividadeId, registradoEm,
                sincronizadaOffline ? " | via fila offline" : "");
    }
}

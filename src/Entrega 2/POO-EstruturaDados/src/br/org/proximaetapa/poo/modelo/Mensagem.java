package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;

/**
 * Comunicado enviado pela ONG (tabela {@code mensagens}). Pode ser destinado a
 * todos os alunos ou apenas aos inscritos em um curso específico.
 *
 * <p>HERANÇA: estende {@link ItemFeed}, de onde herda id, título, corpo, data,
 * {@code equals}/{@code hashCode} e a formatação da linha do feed. O que é
 * exclusivo da mensagem é o <b>destinatário</b> — e é só isso que esta classe
 * precisa declarar.</p>
 *
 * <p>POLIMORFISMO: sobrescreve {@link #rotuloTipo()} para se identificar como
 * "COMUNICADO" (geral) ou "CURSO" (turma específica) no feed unificado.</p>
 */
public class Mensagem extends ItemFeed {

    /** Público-alvo do comunicado (CHECK do banco: 'todos' ou 'curso'). */
    public enum Destinatario { TODOS, CURSO }

    private final Destinatario destinatarioTipo;
    private final Integer cursoId; // null quando o destinatário é TODOS

    private Mensagem(int id, String titulo, String corpo,
                     Destinatario destinatarioTipo, Integer cursoId, LocalDateTime criadoEm) {
        super(id, titulo, corpo, criadoEm);
        this.destinatarioTipo = destinatarioTipo;
        this.cursoId = cursoId;
    }

    /** Comunicado geral, enviado a todos os alunos da ONG. */
    public static Mensagem paraTodos(int id, String titulo, String corpo, LocalDateTime criadoEm) {
        return new Mensagem(id, titulo, corpo, Destinatario.TODOS, null, criadoEm);
    }

    /**
     * Comunicado restrito aos inscritos em um curso.
     * REGRA: o tipo CURSO exige obrigatoriamente um {@code cursoId} — é a
     * mesma consistência que o CHECK + FK garantem no banco.
     */
    public static Mensagem paraCurso(int id, String titulo, String corpo,
                                     int cursoId, LocalDateTime criadoEm) {
        return new Mensagem(id, titulo, corpo, Destinatario.CURSO, cursoId, criadoEm);
    }

    public Destinatario getDestinatarioTipo() { return destinatarioTipo; }

    /** @return o id do curso, ou {@code null} se o comunicado é geral. */
    public Integer getCursoId() { return cursoId; }

    public boolean ehParaTodos() { return destinatarioTipo == Destinatario.TODOS; }

    @Override
    public String rotuloTipo() {
        return destinatarioTipo == Destinatario.TODOS ? "COMUNICADO" : "CURSO";
    }

    @Override
    public String toString() {
        return String.format("Mensagem#%d [%s | %s | destino=%s%s]",
                getId(), getTitulo(), getCriadoEm().format(FORMATO_BR),
                destinatarioTipo, cursoId == null ? "" : " curso=" + cursoId);
    }
}

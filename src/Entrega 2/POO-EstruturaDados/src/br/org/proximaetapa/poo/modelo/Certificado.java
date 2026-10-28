package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Certificado de conclusão de curso (tabela {@code certificados}, com
 * {@code UNIQUE(aluno_id, curso_id)} — um certificado por curso por aluno).
 *
 * <p>REGRA DE NEGÓCIO no próprio modelo: a constante
 * {@link #FREQUENCIA_MINIMA} define o critério de frequência (75%) e
 * {@link #atendeCriterioDeFrequencia(int, int)} é o único lugar do sistema que
 * decide se o aluno tem direito ao documento. O construtor é privado: só é
 * possível criar um Certificado pela fábrica
 * {@link #emitir(int, int, int, int, int)}, que recusa a emissão quando a
 * frequência é insuficiente. Assim não existe certificado inválido em memória.</p>
 */
public class Certificado implements Identificavel {

    /** Frequência mínima exigida pela ONG para emissão do certificado. */
    public static final double FREQUENCIA_MINIMA = 0.75;

    private final int id;
    private final int alunoId;
    private final int cursoId;
    private final int presencasRegistradas;
    private final int totalDeEncontros;
    private final String certificadoUrl;
    private final LocalDateTime emitidoEm;

    private Certificado(int id, int alunoId, int cursoId,
                        int presencasRegistradas, int totalDeEncontros) {
        this.id = id;
        this.alunoId = alunoId;
        this.cursoId = cursoId;
        this.presencasRegistradas = presencasRegistradas;
        this.totalDeEncontros = totalDeEncontros;
        this.certificadoUrl = String.format(
                "https://proximaetapa.org.br/certificados/%d-%d.pdf", cursoId, alunoId);
        this.emitidoEm = LocalDateTime.now();
    }

    /**
     * Calcula a frequência do aluno no curso.
     *
     * @return percentual entre 0.0 e 1.0; 0.0 quando o curso não tem encontros.
     */
    public static double calcularFrequencia(int presencas, int totalDeEncontros) {
        if (totalDeEncontros <= 0) return 0.0;
        return (double) presencas / totalDeEncontros;
    }

    /** O aluno atingiu os 75% de presença exigidos? */
    public static boolean atendeCriterioDeFrequencia(int presencas, int totalDeEncontros) {
        return calcularFrequencia(presencas, totalDeEncontros) >= FREQUENCIA_MINIMA;
    }

    /**
     * Fábrica: emite o certificado <b>somente</b> se o critério de frequência
     * for atendido.
     *
     * @return o certificado emitido, ou {@code null} quando o aluno não atingiu
     *         a frequência mínima (o serviço traduz isso em mensagem ao usuário).
     */
    public static Certificado emitir(int id, int alunoId, int cursoId,
                                     int presencas, int totalDeEncontros) {
        if (!atendeCriterioDeFrequencia(presencas, totalDeEncontros)) {
            return null;
        }
        return new Certificado(id, alunoId, cursoId, presencas, totalDeEncontros);
    }

    @Override
    public int getId() { return id; }

    public int getAlunoId() { return alunoId; }

    public int getCursoId() { return cursoId; }

    public int getPresencasRegistradas() { return presencasRegistradas; }

    public int getTotalDeEncontros() { return totalDeEncontros; }

    public String getCertificadoUrl() { return certificadoUrl; }

    public LocalDateTime getEmitidoEm() { return emitidoEm; }

    public double getFrequencia() {
        return calcularFrequencia(presencasRegistradas, totalDeEncontros);
    }

    /** Chave que espelha o UNIQUE(aluno_id, curso_id) do banco. */
    public static String chaveUnica(int alunoId, int cursoId) {
        return alunoId + ":" + cursoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Certificado)) return false;
        return id == ((Certificado) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Certificado#%d [aluno=%d | curso=%d | frequencia %.0f%% (%d/%d) | %s]",
                id, alunoId, cursoId, getFrequencia() * 100,
                presencasRegistradas, totalDeEncontros, certificadoUrl);
    }
}

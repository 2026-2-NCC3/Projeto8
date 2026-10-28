package br.org.proximaetapa.poo.modelo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Encontro de um curso — aula, oficina ou visita (tabela {@code atividades}).
 * É a atividade cujo {@code qrCode} é lido pelo app para registrar presença.
 *
 * <p>O campo {@code qrCode} é UNIQUE no banco; aqui ele é {@code final} e serve
 * de chave do índice {@code HashMap<String, Atividade>} do
 * {@code RepositorioAtividade}, o que torna o check-in uma busca O(1).</p>
 */
public class Atividade implements Identificavel {

    private final int id;
    private final int cursoId;
    private String titulo;
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private String local;
    private final String qrCode;

    public Atividade(int id, int cursoId, String titulo, LocalDate data,
                     LocalTime horaInicio, LocalTime horaFim, String local, String qrCode) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título da atividade é obrigatório.");
        }
        if (data == null || horaInicio == null || horaFim == null) {
            throw new IllegalArgumentException("Data e horários da atividade são obrigatórios.");
        }
        if (horaFim.isBefore(horaInicio)) {
            throw new IllegalArgumentException("Hora de término anterior à de início.");
        }
        if (qrCode == null || qrCode.isBlank()) {
            throw new IllegalArgumentException("A atividade precisa de um QR Code único.");
        }
        this.id = id;
        this.cursoId = cursoId;
        this.titulo = titulo;
        this.data = data;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.local = local;
        this.qrCode = qrCode;
    }

    @Override
    public int getId() { return id; }

    public int getCursoId() { return cursoId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }

    /** Remarca o encontro (operação comum quando a ONG muda a agenda). */
    public void remarcar(LocalDate novaData, LocalTime novoInicio, LocalTime novoFim) {
        if (novoFim.isBefore(novoInicio)) {
            throw new IllegalArgumentException("Hora de término anterior à de início.");
        }
        this.data = novaData;
        this.horaInicio = novoInicio;
        this.horaFim = novoFim;
    }

    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }

    /** Código lido pelo leitor de QR Code do app no momento do check-in. */
    public String getQrCode() { return qrCode; }

    /** Chave cronológica usada pela {@code AgendaDoAluno} (TreeMap). */
    public LocalDateTime getDataHoraInicio() {
        return LocalDateTime.of(data, horaInicio);
    }

    public long getDuracaoMinutos() {
        return Duration.between(horaInicio, horaFim).toMinutes();
    }

    public boolean jaOcorreu(LocalDateTime referencia) {
        return LocalDateTime.of(data, horaFim).isBefore(referencia);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Atividade)) return false;
        return id == ((Atividade) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Atividade#%d [curso=%d | %s | %s %s-%s | %s | qr=%s]",
                id, cursoId, titulo, data, horaInicio, horaFim, local, qrCode);
    }
}

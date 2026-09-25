package br.org.proximaetapa.poo.modelo;

import java.time.LocalDate;
import java.util.Objects;

/** Um encontro/atividade de um curso (data + horário). */
public class Atividade {
    private final int id;
    private final int cursoId;
    private String titulo;
    private LocalDate data;

    public Atividade(int id, int cursoId, String titulo, LocalDate data) {
        this.id = id;
        this.cursoId = cursoId;
        this.titulo = titulo;
        this.data = data;
    }

    public int getId() { return id; }
    public int getCursoId() { return cursoId; }
    public String getTitulo() { return titulo; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

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
        return String.format("Atividade#%d [curso=%d | %s | %s]", id, cursoId, titulo, data);
    }
}

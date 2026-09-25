package br.org.proximaetapa.poo.modelo;

import java.util.Objects;

public class Curso {
    private final int id;
    private String titulo;
    private String categoria;
    private int cargaHoraria; // horas
    private int vagasTotais;
    private int vagasDisponiveis;

    public Curso(int id, String titulo, String categoria, int cargaHoraria, int vagasTotais) {
        this.id = id;
        this.titulo = titulo;
        this.categoria = categoria;
        this.cargaHoraria = cargaHoraria;
        this.vagasTotais = vagasTotais;
        this.vagasDisponiveis = vagasTotais;
    }

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public int getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(int cargaHoraria) { this.cargaHoraria = cargaHoraria; }
    public int getVagasDisponiveis() { return vagasDisponiveis; }

    public boolean ocuparVaga() {
        if (vagasDisponiveis <= 0) return false;
        vagasDisponiveis--;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Curso)) return false;
        return id == ((Curso) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Curso#%d [%s | %s | %dh | %d/%d vagas]",
            id, titulo, categoria, cargaHoraria, vagasDisponiveis, vagasTotais);
    }
}

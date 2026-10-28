package br.org.proximaetapa.poo.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Curso oferecido pela ONG (tabela {@code cursos}).
 *
 * <p>ENCAPSULAMENTO com regra de negócio: {@code vagasDisponiveis} é privado e
 * <b>não tem setter</b>. A única forma de alterá-lo é por
 * {@link #ocuparVaga()} / {@link #liberarVaga()}, que garantem o invariante
 * {@code 0 <= vagasDisponiveis <= vagasTotais}. Sem isso, qualquer parte do
 * sistema poderia gravar um número negativo de vagas e o app aceitaria
 * inscrição em curso lotado.</p>
 */
public class Curso implements Identificavel {

    private final int id;
    private String titulo;
    private String descricao;
    private String categoria;
    private String local;
    private int cargaHoraria;          // em horas
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private final int vagasTotais;
    private int vagasDisponiveis;
    private final boolean possuiCertificado;
    private boolean ativo;
    private final LocalDateTime criadoEm;

    public Curso(int id, String titulo, String descricao, String categoria, String local,
                 int cargaHoraria, LocalDate dataInicio, LocalDate dataFim,
                 int vagasTotais, boolean possuiCertificado) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título do curso é obrigatório.");
        }
        if (vagasTotais <= 0) {
            throw new IllegalArgumentException("O curso precisa ter ao menos uma vaga.");
        }
        if (dataInicio == null) {
            throw new IllegalArgumentException("Data de início é obrigatória.");
        }
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.local = local;
        this.cargaHoraria = cargaHoraria;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.vagasTotais = vagasTotais;
        this.vagasDisponiveis = vagasTotais;
        this.possuiCertificado = possuiCertificado;
        this.ativo = true;
        this.criadoEm = LocalDateTime.now();
    }

    @Override
    public int getId() { return id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título do curso é obrigatório.");
        }
        this.titulo = titulo;
    }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }

    public int getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(int cargaHoraria) {
        if (cargaHoraria < 0) {
            throw new IllegalArgumentException("Carga horária não pode ser negativa.");
        }
        this.cargaHoraria = cargaHoraria;
    }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public int getVagasTotais() { return vagasTotais; }

    public int getVagasDisponiveis() { return vagasDisponiveis; }

    public boolean isPossuiCertificado() { return possuiCertificado; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }

    /** REGRA: só é possível inscrever enquanto houver vaga e o curso estiver ativo. */
    public boolean temVaga() {
        return ativo && vagasDisponiveis > 0;
    }

    /**
     * Consome uma vaga.
     *
     * @return {@code false} se o curso estiver lotado ou inativo — nesse caso
     *         nada é alterado e o serviço de inscrição recusa o pedido.
     */
    public boolean ocuparVaga() {
        if (!temVaga()) return false;
        vagasDisponiveis--;
        return true;
    }

    /** Devolve uma vaga (usado quando o aluno desiste do curso). */
    public boolean liberarVaga() {
        if (vagasDisponiveis >= vagasTotais) return false;
        vagasDisponiveis++;
        return true;
    }

    public int getVagasOcupadas() { return vagasTotais - vagasDisponiveis; }

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
        return String.format("Curso#%d [%s | %s | %dh | inicio %s | %d/%d vagas livres%s]",
                id, titulo, categoria, cargaHoraria, dataInicio,
                vagasDisponiveis, vagasTotais, ativo ? "" : " | INATIVO");
    }
}

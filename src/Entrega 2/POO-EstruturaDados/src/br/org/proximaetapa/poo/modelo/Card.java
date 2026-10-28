package br.org.proximaetapa.poo.modelo;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Card digital do aluno (RFM08) — a carteirinha exibida na tela inicial do app
 * e apresentada na entrada das oficinas. Corresponde ao endpoint
 * {@code GET /card} do backend: não é uma tabela, é uma <b>projeção</b>
 * calculada a partir do aluno e das inscrições ativas.
 *
 * <p>ABSTRAÇÃO: o Card é imutável e só carrega o que a tela precisa mostrar —
 * nome, escola, série, pontos, nível, cursos ativos e código. Ele
 * deliberadamente <b>não</b> expõe e-mail nem hash de senha do aluno, mesmo
 * tendo sido construído a partir dele. É o encapsulamento protegendo dado
 * sensível em uma tela que pode ser mostrada a terceiros.</p>
 */
public class Card implements Identificavel {

    private final int id;              // mesmo id do aluno
    private final String codigoCard;   // formato PE-000001
    private final String nomeAluno;
    private final String escola;
    private final String serie;
    private final int pontos;
    private final Aluno.Nivel nivel;
    private final int cursosAtivos;
    private final LocalDate validoDesde;

    private Card(Aluno aluno, int cursosAtivos, LocalDate validoDesde) {
        this.id = aluno.getId();
        this.codigoCard = String.format("PE-%06d", aluno.getId());
        this.nomeAluno = aluno.getNome();
        this.escola = aluno.getEscola();
        this.serie = aluno.getSerie();
        this.pontos = aluno.getPontos();
        this.nivel = aluno.getNivel();
        this.cursosAtivos = cursosAtivos;
        this.validoDesde = validoDesde;
    }

    /**
     * Fábrica do card a partir do aluno.
     * Gerar o card só por aqui garante que o código {@code PE-%06d} siga sempre
     * o mesmo formato usado pelo backend e pelo app.
     */
    public static Card paraAluno(Aluno aluno, int cursosAtivos) {
        if (aluno == null) {
            throw new IllegalArgumentException("Card exige um aluno válido.");
        }
        return new Card(aluno, cursosAtivos, LocalDate.now());
    }

    @Override
    public int getId() { return id; }

    public String getCodigoCard() { return codigoCard; }

    public String getNomeAluno() { return nomeAluno; }

    public String getEscola() { return escola; }

    public String getSerie() { return serie; }

    public int getPontos() { return pontos; }

    public Aluno.Nivel getNivel() { return nivel; }

    public int getCursosAtivos() { return cursosAtivos; }

    public LocalDate getValidoDesde() { return validoDesde; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        return id == ((Card) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Card %s | %s | %s - %s | %d pts (%s) | %d curso(s) ativo(s) | valido desde %s",
                codigoCard, nomeAluno, escola, serie, pontos, nivel, cursosAtivos, validoDesde);
    }
}

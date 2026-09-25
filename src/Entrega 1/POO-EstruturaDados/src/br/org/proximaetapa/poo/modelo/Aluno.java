package br.org.proximaetapa.poo.modelo;

import java.util.Objects;

/**
 * Representa um estudante participante da Próxima Etapa.
 * Classe mínima exigida pela Entrega 1 da UC de POO e Estrutura de Dados.
 */
public class Aluno {
    private final int id;
    private String nome;
    private String email;
    private String escola;
    private int pontos;

    public Aluno(int id, String nome, String email, String escola) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.escola = escola;
        this.pontos = 0;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEscola() { return escola; }
    public void setEscola(String escola) { this.escola = escola; }
    public int getPontos() { return pontos; }
    public void adicionarPontos(int quantidade) { this.pontos += quantidade; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Aluno)) return false;
        return id == ((Aluno) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Aluno#%d [%s | %s | %s | %d pts]", id, nome, email, escola, pontos);
    }
}

package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Estudante atendido pela ONG (tabela {@code alunos}).
 *
 * <p>ENCAPSULAMENTO: todos os atributos são privados. {@code id}, {@code email}
 * e {@code criadoEm} não têm setter — o id é a identidade do registro, o e-mail
 * é UNIQUE no banco (trocá-lo é uma operação de serviço, não um simples set) e a
 * data de criação é histórica. Os pontos só mudam por
 * {@link #adicionarPontos(int)}, que valida o valor, nunca por um
 * {@code setPontos} livre que permitiria pontuação negativa.</p>
 */
public class Aluno implements Identificavel {

    /** Faixas de gamificação usadas na tela inicial do app. */
    public enum Nivel {
        EXPLORADOR(0), APRENDIZ(100), DESTAQUE(300), EMBAIXADOR(600);

        private final int pontosMinimos;

        Nivel(int pontosMinimos) { this.pontosMinimos = pontosMinimos; }

        public int getPontosMinimos() { return pontosMinimos; }

        /** Devolve o maior nível cujo mínimo de pontos o aluno já alcançou. */
        public static Nivel paraPontos(int pontos) {
            Nivel atual = EXPLORADOR;
            for (Nivel n : values()) {
                if (pontos >= n.pontosMinimos) atual = n;
            }
            return atual;
        }
    }

    private final int id;
    private String nome;
    private final String email;
    private final String senhaHash;
    private String escola;
    private String serie;
    private String cidade;
    private int pontos;
    private Nivel nivel;
    private String avatarUrl;
    private final LocalDateTime criadoEm;

    public Aluno(int id, String nome, String email, String senhaHash,
                 String escola, String serie, String cidade) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do aluno é obrigatório.");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("E-mail inválido: " + email);
        }
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.escola = escola;
        this.serie = serie;
        this.cidade = cidade;
        this.pontos = 0;
        this.nivel = Nivel.EXPLORADOR;
        this.criadoEm = LocalDateTime.now();
    }

    @Override
    public int getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do aluno é obrigatório.");
        }
        this.nome = nome;
    }

    public String getEmail() { return email; }

    /** Exposto apenas para a camada de autenticação comparar o hash. */
    public String getSenhaHash() { return senhaHash; }

    public String getEscola() { return escola; }
    public void setEscola(String escola) { this.escola = escola; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public int getPontos() { return pontos; }

    public Nivel getNivel() { return nivel; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public LocalDateTime getCriadoEm() { return criadoEm; }

    /**
     * Credita pontos de gamificação (presença confirmada, curso concluído,
     * teste de perfil respondido) e recalcula o nível automaticamente.
     */
    public void adicionarPontos(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A pontuação creditada deve ser positiva.");
        }
        this.pontos += quantidade;
        this.nivel = Nivel.paraPontos(this.pontos);
    }

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
        return String.format("Aluno#%d [%s | %s | %s | %s | %d pts | %s]",
                id, nome, email, escola, serie, pontos, nivel);
    }
}

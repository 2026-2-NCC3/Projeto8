package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Superclasse abstrata de tudo que o aluno <b>recebe</b> e que precisa
 * aparecer no mesmo feed da tela inicial do app: {@link Mensagem} (comunicado
 * enviado pela ONG) e {@link Notificacao} (aviso individual do aluno).
 *
 * <p><b>Por que existe (HERANÇA):</b> as tabelas {@code mensagens} e
 * {@code notificacoes} do banco compartilham exatamente a mesma tríade
 * "título + corpo + data de criação". Em vez de duplicar esses três atributos,
 * os getters, o {@code equals}/{@code hashCode} e a formatação de data nas duas
 * classes, tudo isso fica aqui uma única vez.</p>
 *
 * <p><b>Por que é abstrata (POLIMORFISMO):</b> um ItemFeed "puro" não existe no
 * domínio — todo item é uma mensagem ou uma notificação. O método
 * {@link #rotuloTipo()} é abstrato, então cada subclasse decide como se
 * identifica no feed, e o serviço de feed consegue ordenar e imprimir uma lista
 * {@code List<ItemFeed>} heterogênea sem nenhum {@code if} de tipo.</p>
 */
public abstract class ItemFeed implements Identificavel {

    /** Formato usado na exibição de datas no feed (padrão brasileiro). */
    protected static final DateTimeFormatter FORMATO_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final int id;
    private final String titulo;
    private final String corpo;
    private final LocalDateTime criadoEm;

    protected ItemFeed(int id, String titulo, String corpo, LocalDateTime criadoEm) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Todo item do feed precisa de um título.");
        }
        this.id = id;
        this.titulo = titulo;
        this.corpo = corpo == null ? "" : corpo;
        this.criadoEm = criadoEm == null ? LocalDateTime.now() : criadoEm;
    }

    @Override
    public int getId() { return id; }

    public String getTitulo() { return titulo; }

    public String getCorpo() { return corpo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }

    /**
     * Rótulo que identifica a origem do item no feed.
     * Implementado de forma diferente por cada subclasse (POLIMORFISMO).
     */
    public abstract String rotuloTipo();

    /**
     * Linha pronta para ser exibida no feed unificado da tela inicial.
     * O comportamento é o mesmo para todos os itens, mas o rótulo varia
     * conforme a subclasse — é aqui que a chamada polimórfica acontece.
     */
    public String linhaDoFeed() {
        return String.format("[%s] %s - %s | %s",
                rotuloTipo(), criadoEm.format(FORMATO_BR), titulo, resumoDoCorpo(60));
    }

    /** Corta o corpo para caber na prévia do feed. */
    protected String resumoDoCorpo(int limite) {
        if (corpo.length() <= limite) return corpo;
        return corpo.substring(0, limite - 3) + "...";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // getClass() no lugar de instanceof: uma Mensagem#1 não é a mesma coisa
        // que uma Notificacao#1, apesar de as duas serem ItemFeed com id 1.
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((ItemFeed) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(getClass(), id); }
}

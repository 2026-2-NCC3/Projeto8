package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;

/**
 * Aviso individual entregue a um aluno (tabela {@code notificacoes}): a cópia
 * de um comunicado, um curso novo aberto, um lembrete de agenda ou a emissão de
 * um certificado.
 *
 * <p>HERANÇA: estende {@link ItemFeed} pelos mesmos motivos de
 * {@link Mensagem} — título, corpo e data são idênticos nas duas tabelas. O que
 * é exclusivo daqui é o <b>dono</b> ({@code alunoId}), o <b>tipo</b> e o estado
 * <b>lida/não lida</b>.</p>
 *
 * <p>ENCAPSULAMENTO: {@code lida} começa em {@code false} e só pode virar
 * {@code true} por {@link #marcarComoLida()} — nunca volta para não lida, assim
 * como no app. Não existe {@code setLida(boolean)}.</p>
 */
public class Notificacao extends ItemFeed {

    /** Origem do aviso (coluna {@code tipo} do banco). */
    public enum Tipo {
        MENSAGEM("mensagem"),
        CURSO_NOVO("curso_novo"),
        AGENDA("agenda"),
        CERTIFICADO("certificado");

        private final String codigo;

        Tipo(String codigo) { this.codigo = codigo; }

        public String getCodigo() { return codigo; }
    }

    private final int alunoId;
    private final Integer mensagemId; // null quando não vem de uma Mensagem
    private final Tipo tipo;
    private boolean lida;

    public Notificacao(int id, int alunoId, Tipo tipo, String titulo, String corpo,
                       Integer mensagemId, LocalDateTime criadoEm) {
        super(id, titulo, corpo, criadoEm);
        if (tipo == null) {
            throw new IllegalArgumentException("Toda notificação tem um tipo.");
        }
        this.alunoId = alunoId;
        this.tipo = tipo;
        this.mensagemId = mensagemId;
        this.lida = false;
    }

    /**
     * Cria a notificação individual que entrega uma {@link Mensagem} a um aluno.
     * Reaproveita título e corpo do comunicado, mantendo o vínculo pelo
     * {@code mensagemId} (a FK {@code notificacoes.mensagem_id}).
     */
    public static Notificacao deMensagem(int id, int alunoId, Mensagem mensagem) {
        return new Notificacao(id, alunoId, Tipo.MENSAGEM,
                mensagem.getTitulo(), mensagem.getCorpo(), mensagem.getId(), LocalDateTime.now());
    }

    public int getAlunoId() { return alunoId; }

    /** @return id da mensagem de origem, ou {@code null} se foi gerada pelo sistema. */
    public Integer getMensagemId() { return mensagemId; }

    public Tipo getTipo() { return tipo; }

    public boolean isLida() { return lida; }

    /** @return {@code true} na primeira vez; {@code false} se já estava lida. */
    public boolean marcarComoLida() {
        if (lida) return false;
        lida = true;
        return true;
    }

    @Override
    public String rotuloTipo() {
        return "AVISO/" + tipo.getCodigo();
    }

    /** Acrescenta o marcador de não lida na linha herdada do feed. */
    @Override
    public String linhaDoFeed() {
        return super.linhaDoFeed() + (lida ? "" : "  <= nao lida");
    }

    @Override
    public String toString() {
        return String.format("Notificacao#%d [aluno=%d | %s | %s | %s | %s]",
                getId(), alunoId, tipo.getCodigo(), getTitulo(),
                getCriadoEm().format(FORMATO_BR), lida ? "lida" : "nao lida");
    }
}

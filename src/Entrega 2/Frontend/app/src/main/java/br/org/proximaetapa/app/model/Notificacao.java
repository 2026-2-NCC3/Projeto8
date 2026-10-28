package br.org.proximaetapa.app.model;

/** Notificação na caixa do aluno (RFM13). */
public class Notificacao {
    public int id;
    public Integer mensagem_id;
    public String tipo;      // mensagem | curso_novo | agenda | certificado
    public String titulo;
    public String corpo;
    public int lida;         // 0 ou 1 (o backend usa INTEGER)
    public String criado_em;
}

package br.org.proximaetapa.app.model;

/** Uma fala do chat de dúvidas (RFM12). */
public class ChatMensagem {
    public int id;
    public String autor;   // "aluno" ou "equipe"
    public String texto;
    public String criado_em;
}

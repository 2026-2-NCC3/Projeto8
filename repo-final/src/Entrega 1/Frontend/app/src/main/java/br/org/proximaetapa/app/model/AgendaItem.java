package br.org.proximaetapa.app.model;

/** Um encontro/atividade na agenda individual do aluno (RFM05). */
public class AgendaItem {
    public int id;
    public String titulo;
    public String data;
    public String hora_inicio;
    public String hora_fim;
    public String local;
    public String curso;
    public int curso_id;
    public boolean presenca_confirmada;
}

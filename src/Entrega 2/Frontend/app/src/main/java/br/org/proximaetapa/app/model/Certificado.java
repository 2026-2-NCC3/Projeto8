package br.org.proximaetapa.app.model;

/** Certificado emitido para o aluno em um curso concluído (RFM09). */
public class Certificado {
    public int id;
    public int curso_id;
    public String certificado_url;
    public String emitido_em;
    public String curso;
    public int carga_horaria;
}

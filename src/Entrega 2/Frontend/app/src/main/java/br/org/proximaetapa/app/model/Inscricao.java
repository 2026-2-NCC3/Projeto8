package br.org.proximaetapa.app.model;

/** Vínculo entre aluno e curso, como vem no pacote de sincronização. */
public class Inscricao {
    public int id;
    public int curso_id;
    public String status;   // inscrito | cursando | concluido | desistente
    public String criado_em;
}

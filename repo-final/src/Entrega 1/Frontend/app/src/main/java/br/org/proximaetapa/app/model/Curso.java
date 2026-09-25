package br.org.proximaetapa.app.model;

import java.io.Serializable;

/** Representa um curso oferecido pela Próxima Etapa (tabela `cursos` da API). */
public class Curso implements Serializable {
    public int id;
    public String titulo;
    public String descricao;
    public String categoria;
    public String local;
    public int carga_horaria;      // dado numérico exigido pela UC de Dispositivos Móveis
    public String data_inicio;
    public String data_fim;
    public int vagas_totais;
    public int vagas_disponiveis;
    public int possui_certificado;
    public String status_inscricao; // presente quando vem de /cursos/inscritos/meus
}

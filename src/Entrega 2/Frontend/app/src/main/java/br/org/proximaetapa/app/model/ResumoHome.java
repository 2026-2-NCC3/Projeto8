package br.org.proximaetapa.app.model;

import java.util.List;

/** Resumo da jornada do aluno exibido na tela inicial (RFM02). */
public class ResumoHome {
    public Aluno aluno;
    public List<AgendaItem> proximasAtividades;
    public int cursosInscritos;
    public int notificacoesNaoLidas;
}

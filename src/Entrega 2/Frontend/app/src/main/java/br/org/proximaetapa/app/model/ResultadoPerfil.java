package br.org.proximaetapa.app.model;

/** Resultado do teste de perfil devolvido pela API (RFM10). */
public class ResultadoPerfil {
    public String perfil;
    public String descricao;
    public String aviso;
    public String perfil_resultado; // nome usado no histórico (GET /perfil-teste/resultados)
    public String concluido_em;
}

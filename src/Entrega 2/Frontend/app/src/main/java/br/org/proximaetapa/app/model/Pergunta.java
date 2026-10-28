package br.org.proximaetapa.app.model;

import java.io.Serializable;
import java.util.List;

/** Pergunta do teste de perfil, com suas alternativas (RFM10). */
public class Pergunta implements Serializable {
    public int id;
    public String texto;
    public int ordem;
    public List<Alternativa> alternativas;
}

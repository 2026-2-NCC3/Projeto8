package br.org.proximaetapa.app.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Corpo de POST /perfil-teste/respostas.
 * A API espera { "respostas": { "<pergunta_id>": <alternativa_id> } }.
 */
public class RespostasPerfil {
    public Map<String, Integer> respostas = new HashMap<>();

    public void responder(int perguntaId, int alternativaId) {
        respostas.put(String.valueOf(perguntaId), alternativaId);
    }
}

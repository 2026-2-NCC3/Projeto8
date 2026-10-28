package br.org.proximaetapa.app.model;

import java.util.List;

/** Resposta de POST /sync/presencas. */
public class RespostaEnvioPresencas {
    public String servidor_em;
    public int recebidas;
    public int sincronizadas;
    public List<ResultadoEnvioPresenca> resultados;
}

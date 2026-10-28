package br.org.proximaetapa.app.model;

import java.util.List;

/** Corpo de POST /sync/presencas: a fila de check-ins acumulada offline. */
public class EnvioPresencas {
    public List<PresencaPendenteEnvio> presencas;

    public EnvioPresencas(List<PresencaPendenteEnvio> presencas) {
        this.presencas = presencas;
    }
}

package br.org.proximaetapa.app;

import android.app.Application;

import br.org.proximaetapa.app.util.Notificador;

/**
 * Ponto de partida do processo. Serve para preparar, uma única vez, o que o
 * app inteiro precisa — hoje, o canal de notificações (RFM13).
 */
public class ProximaEtapaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Notificador.criarCanal(this);
    }
}

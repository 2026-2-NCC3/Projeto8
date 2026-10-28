package br.org.proximaetapa.app.model;

/** Corpo de POST /chat. */
public class EnvioChat {
    public String texto;

    public EnvioChat(String texto) {
        this.texto = texto;
    }
}

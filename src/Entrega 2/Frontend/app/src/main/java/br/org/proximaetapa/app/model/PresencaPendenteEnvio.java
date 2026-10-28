package br.org.proximaetapa.app.model;

/** Um check-in feito offline, no formato que o servidor espera receber. */
public class PresencaPendenteEnvio {
    public String uuid_local;
    public String qr_code;
    public String registrado_em;

    public PresencaPendenteEnvio(String uuidLocal, String qrCode, String registradoEm) {
        this.uuid_local = uuidLocal;
        this.qr_code = qrCode;
        this.registrado_em = registradoEm;
    }
}

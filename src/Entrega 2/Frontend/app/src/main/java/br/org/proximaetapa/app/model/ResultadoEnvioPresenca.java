package br.org.proximaetapa.app.model;

/**
 * Status de UM item da fila, devolvido por POST /sync/presencas.
 * O app usa esse status para decidir o que apagar da fila local.
 */
public class ResultadoEnvioPresenca {
    public String uuid_local;
    public String status;   // sincronizado | duplicado | nao_inscrito | qr_invalido | erro
    public String erro;
    public String atividade;
    public Integer atividade_id;
    public String data;

    /**
     * Tanto "sincronizado" quanto "duplicado" significam que o servidor já tem
     * esse check-in — nos dois casos o item sai da fila local.
     */
    public boolean resolvido() {
        return "sincronizado".equals(status) || "duplicado".equals(status);
    }
}

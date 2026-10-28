package br.org.proximaetapa.app.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda a sessão do aluno no próprio aparelho (RNF: segurança / armazenamento
 * de tokens). Para produção, o passo seguinte seria EncryptedSharedPreferences.
 *
 * Na Entrega 2 ganhou também o carimbo da última sincronização, que é o que
 * permite ao app pedir ao servidor apenas o que mudou.
 */
public class SessionManager {
    private static final String PREFS = "proxima_etapa_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ALUNO_NOME = "aluno_nome";
    private static final String KEY_ALUNO_ID = "aluno_id";
    private static final String KEY_ULTIMA_SINC = "ultima_sincronizacao";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void salvarSessao(String token, int alunoId, String nome) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putInt(KEY_ALUNO_ID, alunoId)
                .putString(KEY_ALUNO_NOME, nome)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public int getAlunoId() {
        return prefs.getInt(KEY_ALUNO_ID, 0);
    }

    public String getNomeAluno() {
        return prefs.getString(KEY_ALUNO_NOME, "");
    }

    public boolean estaLogado() {
        return getToken() != null;
    }

    /**
     * Carimbo devolvido pelo servidor na última sincronização bem-sucedida.
     * Guardamos o horário do SERVIDOR, não o do celular: se o relógio do
     * aparelho estiver adiantado, usar a hora local faria o app pular
     * registros que chegaram nesse intervalo.
     */
    public String getUltimaSincronizacao() {
        return prefs.getString(KEY_ULTIMA_SINC, null);
    }

    public void salvarUltimaSincronizacao(String carimbo) {
        prefs.edit().putString(KEY_ULTIMA_SINC, carimbo).apply();
    }

    /** Força a próxima sincronização a baixar o pacote completo. */
    public void limparUltimaSincronizacao() {
        prefs.edit().remove(KEY_ULTIMA_SINC).apply();
    }

    public void limpar() {
        prefs.edit().clear().apply();
    }
}

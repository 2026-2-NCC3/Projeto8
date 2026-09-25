package br.org.proximaetapa.app.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Armazenamento seguro e simples do token de sessão (RNF: segurança /
 * armazenamento de tokens). Para produção, considerar EncryptedSharedPreferences.
 */
public class SessionManager {
    private static final String PREFS = "proxima_etapa_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ALUNO_NOME = "aluno_nome";
    private static final String KEY_ALUNO_ID = "aluno_id";

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

    public String getNomeAluno() {
        return prefs.getString(KEY_ALUNO_NOME, "");
    }

    public boolean estaLogado() {
        return getToken() != null;
    }

    public void limpar() {
        prefs.edit().clear().apply();
    }
}

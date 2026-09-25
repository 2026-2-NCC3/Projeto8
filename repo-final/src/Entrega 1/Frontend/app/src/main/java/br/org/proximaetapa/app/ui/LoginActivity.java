package br.org.proximaetapa.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.data.SessionManager;
import br.org.proximaetapa.app.model.ErroResposta;
import br.org.proximaetapa.app.model.LoginRequest;
import br.org.proximaetapa.app.model.LoginResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/** RFM01 - Autenticação. Tela inicial do fluxo Login → Cursos → Agenda. */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail;
    private TextInputEditText edtSenha;
    private Button btnEntrar;
    private ProgressBar progressLogin;
    private TextView txtErro;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);

        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        progressLogin = findViewById(R.id.progressLogin);
        txtErro = findViewById(R.id.txtErro);

        btnEntrar.setOnClickListener(v -> tentarLogin());

        // Se já existe sessão válida, pula direto para a lista de cursos.
        if (session.estaLogado()) {
            abrirCursos();
        }
    }

    private void tentarLogin() {
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
        String senha = edtSenha.getText() != null ? edtSenha.getText().toString().trim() : "";

        if (email.isEmpty() || senha.isEmpty()) {
            mostrarErro("Preencha e-mail e senha.");
            return;
        }

        mostrarCarregando(true);
        ApiClient.get(this).login(new LoginRequest(email, senha)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                mostrarCarregando(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse corpo = response.body();
                    session.salvarSessao(corpo.token, corpo.aluno.id, corpo.aluno.nome);
                    abrirCursos();
                } else {
                    mostrarErro(extrairMensagemErro(response.errorBody(), getString(R.string.erro_login)));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                mostrarCarregando(false);
                mostrarErro(getString(R.string.erro_rede));
            }
        });
    }

    private String extrairMensagemErro(ResponseBody errorBody, String padrao) {
        if (errorBody == null) return padrao;
        try {
            ErroResposta erro = new Gson().fromJson(errorBody.string(), ErroResposta.class);
            return erro != null && erro.erro != null ? erro.erro : padrao;
        } catch (Exception e) {
            return padrao;
        }
    }

    private void mostrarCarregando(boolean carregando) {
        progressLogin.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!carregando);
    }

    private void mostrarErro(String mensagem) {
        txtErro.setText(mensagem);
        txtErro.setVisibility(View.VISIBLE);
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    private void abrirCursos() {
        Intent intent = new Intent(this, CursosActivity.class);
        startActivity(intent);
        finish();
    }
}

package br.org.proximaetapa.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.data.SessionManager;
import br.org.proximaetapa.app.model.ErroResposta;
import br.org.proximaetapa.app.model.LoginRequest;
import br.org.proximaetapa.app.model.LoginResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** RFM01 - Autenticação. Porta de entrada do fluxo Login → Início → Cursos → Agenda. */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail;
    private TextInputEditText edtSenha;
    private Button btnEntrar;
    private ProgressBar progressLogin;
    private TextView txtErro;
    private TextView txtStatusSincronizacao;
    private SessionManager session;
    private Repositorio repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);
        repositorio = Repositorio.obter(this);

        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        progressLogin = findViewById(R.id.progressLogin);
        txtErro = findViewById(R.id.txtErro);
        txtStatusSincronizacao = findViewById(R.id.txtStatusSincronizacao);

        btnEntrar.setOnClickListener(v -> tentarLogin());

        // Sessão ainda válida: entra direto, sem pedir senha de novo.
        if (session.estaLogado()) {
            abrirInicio();
        }
    }

    private void tentarLogin() {
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
        String senha = edtSenha.getText() != null ? edtSenha.getText().toString().trim() : "";

        if (email.isEmpty() || senha.isEmpty()) {
            mostrarErro("Preencha e-mail e senha.");
            return;
        }

        if (!repositorio.temConexao()) {
            // O primeiro login precisa de internet: é ele que traz o token e o
            // pacote inicial de dados. Depois disso o app abre offline.
            mostrarErro("Você está sem internet. O primeiro acesso precisa de conexão.");
            return;
        }

        mostrarCarregando(true);
        ApiClient.get(this).login(new LoginRequest(email, senha)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse corpo = response.body();
                    session.salvarSessao(corpo.token, corpo.aluno.id, corpo.aluno.nome);
                    // Conta nova (ou reinstalação): força o pacote completo.
                    session.limparUltimaSincronizacao();
                    baixarDadosIniciais();
                } else {
                    mostrarCarregando(false);
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

    /**
     * Primeira sincronização logo após o login: enche o banco local para que
     * as próximas telas abram instantaneamente e funcionem sem internet.
     */
    private void baixarDadosIniciais() {
        txtStatusSincronizacao.setVisibility(View.VISIBLE);
        txtStatusSincronizacao.setText(R.string.sincronizando_dados);

        repositorio.sincronizar(new Repositorio.Callback<Repositorio.ResultadoSincronizacao>() {
            @Override
            public void sucesso(Repositorio.ResultadoSincronizacao resultado) {
                mostrarCarregando(false);
                abrirInicio();
            }

            @Override
            public void falha(String mensagem) {
                // A sessão já está salva. Deixar entrar é melhor do que travar
                // no login: a sincronização tenta de novo na tela inicial.
                mostrarCarregando(false);
                abrirInicio();
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
        if (!carregando) txtStatusSincronizacao.setVisibility(View.GONE);
    }

    private void mostrarErro(String mensagem) {
        txtErro.setText(mensagem);
        txtErro.setVisibility(View.VISIBLE);
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    private void abrirInicio() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}

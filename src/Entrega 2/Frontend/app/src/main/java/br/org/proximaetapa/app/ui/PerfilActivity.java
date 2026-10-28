package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.data.local.AlunoLocal;
import br.org.proximaetapa.app.model.Aluno;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RFM14 — Perfil do aluno.
 *
 * Nome e e-mail aparecem, mas não são editáveis aqui: são os dados que
 * identificam o aluno no cadastro da ONG e mudam por lá. O aluno edita o que
 * é dele manter atualizado — escola, série e cidade.
 */
public class PerfilActivity extends AppCompatActivity {

    private TextView txtNome;
    private TextView txtEmail;
    private TextInputEditText edtEscola;
    private TextInputEditText edtSerie;
    private TextInputEditText edtCidade;
    private Button btnSalvar;
    private ProgressBar progress;
    private Repositorio repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        repositorio = Repositorio.obter(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        txtNome = findViewById(R.id.txtNomePerfil);
        txtEmail = findViewById(R.id.txtEmailPerfil);
        edtEscola = findViewById(R.id.edtEscola);
        edtSerie = findViewById(R.id.edtSerie);
        edtCidade = findViewById(R.id.edtCidade);
        btnSalvar = findViewById(R.id.btnSalvarPerfil);
        progress = findViewById(R.id.progressPerfil);

        btnSalvar.setOnClickListener(v -> salvar());

        carregarDoBancoLocal();
    }

    private void carregarDoBancoLocal() {
        repositorio.buscarAluno(new Repositorio.Callback<AlunoLocal>() {
            @Override
            public void sucesso(AlunoLocal aluno) {
                if (aluno == null) return;
                txtNome.setText(aluno.nome);
                txtEmail.setText(aluno.email);
                edtEscola.setText(aluno.escola);
                edtSerie.setText(aluno.serie);
                edtCidade.setText(aluno.cidade);
            }

            @Override
            public void falha(String mensagem) {
            }
        });
    }

    private void salvar() {
        if (!repositorio.temConexao()) {
            // Edição de perfil exige rede: salvar só localmente criaria duas
            // versões do mesmo dado e um conflito na próxima sincronização.
            Toast.makeText(this, R.string.perfil_precisa_internet, Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, String> campos = new HashMap<>();
        campos.put("escola", texto(edtEscola));
        campos.put("serie", texto(edtSerie));
        campos.put("cidade", texto(edtCidade));

        progress.setVisibility(View.VISIBLE);
        btnSalvar.setEnabled(false);

        ApiClient.get(this).atualizarPerfil(campos).enqueue(new Callback<Aluno>() {
            @Override
            public void onResponse(Call<Aluno> call, Response<Aluno> response) {
                progress.setVisibility(View.GONE);
                btnSalvar.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(PerfilActivity.this, R.string.perfil_salvo, Toast.LENGTH_SHORT).show();
                    // Traz o perfil atualizado para o banco local.
                    repositorio.sincronizar(new Repositorio.Callback<Repositorio.ResultadoSincronizacao>() {
                        @Override
                        public void sucesso(Repositorio.ResultadoSincronizacao r) {
                            carregarDoBancoLocal();
                        }

                        @Override
                        public void falha(String m) {
                        }
                    });
                } else {
                    Toast.makeText(PerfilActivity.this, R.string.erro_salvar_perfil, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Aluno> call, Throwable t) {
                progress.setVisibility(View.GONE);
                btnSalvar.setEnabled(true);
                Toast.makeText(PerfilActivity.this, R.string.erro_rede, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}

package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.Map;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.model.Pergunta;
import br.org.proximaetapa.app.model.RespostasPerfil;
import br.org.proximaetapa.app.model.ResultadoPerfil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RFM10 — Teste de perfil educacional/profissional.
 *
 * O resultado é orientação vocacional, não diagnóstico: o próprio documento do
 * projeto exige que isso fique explícito, e a tela repete o aviso que a API
 * devolve junto com o resultado.
 */
public class TestePerfilActivity extends AppCompatActivity {

    private PerguntaAdapter adapter;
    private RecyclerView recycler;
    private Button btnEnviar;
    private ProgressBar progress;
    private TextView txtAviso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teste_perfil);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerPerguntas);
        btnEnviar = findViewById(R.id.btnEnviarRespostas);
        progress = findViewById(R.id.progressTeste);
        txtAviso = findViewById(R.id.txtAvisoTeste);

        adapter = new PerguntaAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnEnviar.setOnClickListener(v -> enviarRespostas());

        carregarPerguntas();
    }

    private void carregarPerguntas() {
        if (!Repositorio.obter(this).temConexao()) {
            // O teste não é guardado offline: são poucas perguntas, e o
            // resultado precisa ser calculado pelo servidor.
            txtAviso.setVisibility(View.VISIBLE);
            txtAviso.setText(R.string.teste_precisa_internet);
            btnEnviar.setEnabled(false);
            return;
        }

        progress.setVisibility(View.VISIBLE);
        ApiClient.get(this).perguntasPerfil().enqueue(new Callback<List<Pergunta>>() {
            @Override
            public void onResponse(Call<List<Pergunta>> call, Response<List<Pergunta>> response) {
                progress.setVisibility(View.GONE);
                List<Pergunta> perguntas = response.body();
                if (!response.isSuccessful() || perguntas == null || perguntas.isEmpty()) {
                    txtAviso.setVisibility(View.VISIBLE);
                    txtAviso.setText(R.string.teste_indisponivel);
                    btnEnviar.setEnabled(false);
                    return;
                }
                adapter.atualizar(perguntas);
                btnEnviar.setEnabled(true);
            }

            @Override
            public void onFailure(Call<List<Pergunta>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                txtAviso.setVisibility(View.VISIBLE);
                txtAviso.setText(R.string.erro_rede);
                btnEnviar.setEnabled(false);
            }
        });
    }

    private void enviarRespostas() {
        if (!adapter.respondeuTudo()) {
            int faltam = adapter.quantasFaltam();
            Toast.makeText(this,
                    getResources().getQuantityString(R.plurals.faltam_perguntas, faltam, faltam),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        RespostasPerfil corpo = new RespostasPerfil();
        for (Map.Entry<Integer, Integer> resposta : adapter.getRespostas().entrySet()) {
            corpo.responder(resposta.getKey(), resposta.getValue());
        }

        progress.setVisibility(View.VISIBLE);
        btnEnviar.setEnabled(false);

        ApiClient.get(this).responderPerfil(corpo).enqueue(new Callback<ResultadoPerfil>() {
            @Override
            public void onResponse(Call<ResultadoPerfil> call, Response<ResultadoPerfil> response) {
                progress.setVisibility(View.GONE);
                btnEnviar.setEnabled(true);

                ResultadoPerfil resultado = response.body();
                if (!response.isSuccessful() || resultado == null) {
                    Toast.makeText(TestePerfilActivity.this, R.string.erro_enviar_respostas,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrarResultado(resultado);
            }

            @Override
            public void onFailure(Call<ResultadoPerfil> call, Throwable t) {
                progress.setVisibility(View.GONE);
                btnEnviar.setEnabled(true);
                Toast.makeText(TestePerfilActivity.this, R.string.erro_rede, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarResultado(ResultadoPerfil resultado) {
        String aviso = resultado.aviso == null ? getString(R.string.aviso_orientacao) : resultado.aviso;
        String mensagem = resultado.descricao + "\n\n" + aviso;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.resultado_perfil, resultado.perfil))
                .setMessage(mensagem)
                .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}

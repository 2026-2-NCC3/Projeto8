package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.model.Curso;
import br.org.proximaetapa.app.model.ErroResposta;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Detalhe de um curso, aberto a partir da lista.
 *
 * Os dados vêm na própria Intent — não há chamada de rede só para exibir a
 * tela. A rede só entra se o aluno decidir se inscrever (RFM03).
 */
public class CursoDetalheActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO = "extra_curso";

    private Button btnInscrever;
    private TextView txtStatusInscricao;
    private Curso curso;
    private Repositorio repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curso_detalhe);

        repositorio = Repositorio.obter(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        curso = (Curso) getIntent().getSerializableExtra(EXTRA_CURSO);

        TextView txtTitulo = findViewById(R.id.txtTitulo);
        TextView txtDescricao = findViewById(R.id.txtDescricao);
        TextView txtDetalhes = findViewById(R.id.txtDetalhes);
        txtStatusInscricao = findViewById(R.id.txtStatusInscricao);
        btnInscrever = findViewById(R.id.btnInscrever);

        if (curso == null) {
            finish();
            return;
        }

        txtTitulo.setText(curso.titulo);
        txtDescricao.setText(curso.descricao);
        txtDetalhes.setText(String.format(
                "Local: %s%nCarga horária: %dh%nVagas: %d de %d disponíveis%nPeríodo: %s a %s",
                curso.local, curso.carga_horaria, curso.vagas_disponiveis, curso.vagas_totais,
                br.org.proximaetapa.app.util.Formato.data(curso.data_inicio),
                br.org.proximaetapa.app.util.Formato.data(curso.data_fim)));

        atualizarBotao();
        btnInscrever.setOnClickListener(v -> inscrever());
    }

    /** O botão muda conforme a situação: já inscrito, sem vaga, ou disponível. */
    private void atualizarBotao() {
        if (curso.status_inscricao != null) {
            btnInscrever.setVisibility(View.GONE);
            txtStatusInscricao.setVisibility(View.VISIBLE);
            txtStatusInscricao.setText(getString(R.string.ja_inscrito, curso.status_inscricao));
        } else if (curso.vagas_disponiveis <= 0) {
            btnInscrever.setEnabled(false);
            btnInscrever.setText(R.string.sem_vagas);
        } else {
            btnInscrever.setEnabled(true);
            btnInscrever.setText(R.string.inscrever_se);
        }
    }

    private void inscrever() {
        if (!repositorio.temConexao()) {
            // Inscrição mexe em vaga, que é um recurso disputado: não dá para
            // confirmar offline e descobrir depois que acabou.
            Toast.makeText(this, R.string.inscricao_precisa_internet, Toast.LENGTH_LONG).show();
            return;
        }

        btnInscrever.setEnabled(false);
        ApiClient.get(this).inscrever(curso.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CursoDetalheActivity.this, R.string.inscricao_feita, Toast.LENGTH_LONG).show();
                    curso.status_inscricao = "inscrito";
                    atualizarBotao();
                    // A agenda do aluno muda com a inscrição: sincroniza para
                    // os encontros do curso novo aparecerem.
                    repositorio.sincronizar(new Repositorio.Callback<Repositorio.ResultadoSincronizacao>() {
                        @Override
                        public void sucesso(Repositorio.ResultadoSincronizacao r) {
                        }

                        @Override
                        public void falha(String m) {
                        }
                    });
                } else {
                    btnInscrever.setEnabled(true);
                    Toast.makeText(CursoDetalheActivity.this,
                            extrairErro(response.errorBody()), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnInscrever.setEnabled(true);
                Toast.makeText(CursoDetalheActivity.this, R.string.erro_rede, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extrairErro(ResponseBody corpo) {
        if (corpo == null) return getString(R.string.erro_inscricao);
        try {
            ErroResposta erro = new Gson().fromJson(corpo.string(), ErroResposta.class);
            return erro != null && erro.erro != null ? erro.erro : getString(R.string.erro_inscricao);
        } catch (Exception e) {
            return getString(R.string.erro_inscricao);
        }
    }
}

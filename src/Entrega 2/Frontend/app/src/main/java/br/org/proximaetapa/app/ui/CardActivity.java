package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.data.local.AlunoLocal;
import br.org.proximaetapa.app.model.CardAluno;
import br.org.proximaetapa.app.util.Formato;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RFM08 — Card digital do participante.
 *
 * É o documento que identifica o aluno como participante da Próxima Etapa, e
 * ele pode precisar mostrá-lo na portaria de um evento sem ter internet. Por
 * isso a tela desenha primeiro com os dados guardados no celular e só depois
 * tenta atualizar pela rede.
 */
public class CardActivity extends AppCompatActivity {

    private TextView txtNome;
    private TextView txtCodigo;
    private TextView txtEscola;
    private TextView txtNivel;
    private TextView txtCursosAtivos;
    private TextView txtValidade;
    private TextView txtAvisoOffline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        txtNome = findViewById(R.id.txtNomeCard);
        txtCodigo = findViewById(R.id.txtCodigoCard);
        txtEscola = findViewById(R.id.txtEscolaCard);
        txtNivel = findViewById(R.id.txtNivelCard);
        txtCursosAtivos = findViewById(R.id.txtCursosAtivosCard);
        txtValidade = findViewById(R.id.txtValidadeCard);
        txtAvisoOffline = findViewById(R.id.txtAvisoOffline);

        desenharComDadosLocais();
        atualizarPelaRede();
    }

    /** Preenche o card com o que já está no celular. */
    private void desenharComDadosLocais() {
        Repositorio.obter(this).buscarAluno(new Repositorio.Callback<AlunoLocal>() {
            @Override
            public void sucesso(AlunoLocal aluno) {
                if (aluno == null) return;
                txtNome.setText(aluno.nome);
                txtEscola.setText(aluno.escola);
                txtNivel.setText(getString(R.string.nivel_e_pontos, aluno.nivel, aluno.pontos));
                // Mesmo formato gerado pelo servidor, para o código não mudar
                // de aparência quando a tela abre offline.
                txtCodigo.setText(String.format("PE-%06d", aluno.id));
            }

            @Override
            public void falha(String mensagem) {
            }
        });
    }

    /** Busca a versão autoritativa do card quando há internet. */
    private void atualizarPelaRede() {
        if (!Repositorio.obter(this).temConexao()) {
            txtAvisoOffline.setVisibility(View.VISIBLE);
            return;
        }

        ApiClient.get(this).card().enqueue(new Callback<CardAluno>() {
            @Override
            public void onResponse(Call<CardAluno> call, Response<CardAluno> response) {
                CardAluno card = response.body();
                if (!response.isSuccessful() || card == null) return;

                txtNome.setText(card.nome);
                txtCodigo.setText(card.codigoCard);
                txtEscola.setText(card.escola);
                txtNivel.setText(getString(R.string.nivel_e_pontos, card.nivel, card.pontos));
                txtCursosAtivos.setText(getResources().getQuantityString(
                        R.plurals.cursos_ativos, card.cursosAtivos, card.cursosAtivos));
                txtValidade.setText(getString(R.string.valido_desde, Formato.data(card.validoDesde)));
                txtAvisoOffline.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<CardAluno> call, Throwable t) {
                // O card já está desenhado com os dados locais; a falha de rede
                // não precisa virar erro na cara do aluno.
                txtAvisoOffline.setVisibility(View.VISIBLE);
            }
        });
    }
}

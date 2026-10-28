package br.org.proximaetapa.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.model.Curso;

/**
 * RFM03 — Cursos.
 *
 * Diferença em relação à Entrega 1: a lista agora vem do banco local, não da
 * rede. A tela abre preenchida mesmo offline, e a rede serve só para
 * atualizar o que está guardado.
 */
public class CursosActivity extends AppCompatActivity {

    private RecyclerView recyclerCursos;
    private SwipeRefreshLayout swipeRefresh;
    private TextView txtVazio;
    private TextView txtAvisoOffline;
    private CursoAdapter adapter;
    private Repositorio repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cursos);

        repositorio = Repositorio.obter(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerCursos = findViewById(R.id.recyclerCursos);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        txtVazio = findViewById(R.id.txtVazio);
        txtAvisoOffline = findViewById(R.id.txtAvisoOffline);

        adapter = new CursoAdapter(curso -> {
            Intent intent = new Intent(CursosActivity.this, CursoDetalheActivity.class);
            intent.putExtra(CursoDetalheActivity.EXTRA_CURSO, curso);
            startActivity(intent);
        });
        recyclerCursos.setLayoutManager(new LinearLayoutManager(this));
        recyclerCursos.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::sincronizar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarDoBancoLocal();
    }

    private void carregarDoBancoLocal() {
        repositorio.listarCursos(new Repositorio.Callback<List<Curso>>() {
            @Override
            public void sucesso(List<Curso> cursos) {
                adapter.atualizar(cursos);
                boolean vazio = cursos.isEmpty();
                txtVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
                recyclerCursos.setVisibility(vazio ? View.GONE : View.VISIBLE);
                txtAvisoOffline.setVisibility(repositorio.temConexao() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void falha(String mensagem) {
                txtVazio.setVisibility(View.VISIBLE);
            }
        });
    }

    private void sincronizar() {
        if (!repositorio.temConexao()) {
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, R.string.modo_offline, Toast.LENGTH_SHORT).show();
            return;
        }

        repositorio.sincronizar(new Repositorio.Callback<Repositorio.ResultadoSincronizacao>() {
            @Override
            public void sucesso(Repositorio.ResultadoSincronizacao resultado) {
                swipeRefresh.setRefreshing(false);
                carregarDoBancoLocal();
            }

            @Override
            public void falha(String mensagem) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CursosActivity.this, mensagem, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

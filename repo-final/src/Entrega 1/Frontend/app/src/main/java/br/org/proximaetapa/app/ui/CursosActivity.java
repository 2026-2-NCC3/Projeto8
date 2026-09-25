package br.org.proximaetapa.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.data.SessionManager;
import br.org.proximaetapa.app.model.Curso;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Button;

/**
 * RFM03 - Cursos. Primeira tela funcional após o login, parte do fluxo
 * Login → Cursos → Agenda descrito no plano do Projeto Interdisciplinar.
 */
public class CursosActivity extends AppCompatActivity {

    private RecyclerView recyclerCursos;
    private SwipeRefreshLayout swipeRefresh;
    private CursoAdapter adapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cursos);

        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerCursos = findViewById(R.id.recyclerCursos);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        Button btnVerAgenda = findViewById(R.id.btnVerAgenda);

        adapter = new CursoAdapter(curso -> {
            Intent intent = new Intent(CursosActivity.this, CursoDetalheActivity.class);
            intent.putExtra(CursoDetalheActivity.EXTRA_CURSO, curso);
            startActivity(intent);
        });
        recyclerCursos.setLayoutManager(new LinearLayoutManager(this));
        recyclerCursos.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::carregarCursos);
        btnVerAgenda.setOnClickListener(v -> startActivity(new Intent(this, AgendaActivity.class)));

        carregarCursos();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cursos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sair) {
            confirmarSaida();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmarSaida() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.sair)
            .setMessage(R.string.confirmar_sair)
            .setPositiveButton(R.string.sair, (dialog, which) -> sair())
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void sair() {
        session.limpar();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void carregarCursos() {
        swipeRefresh.setRefreshing(true);
        ApiClient.get(this).listarCursos().enqueue(new Callback<List<Curso>>() {
            @Override
            public void onResponse(Call<List<Curso>> call, Response<List<Curso>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.atualizar(response.body());
                } else {
                    Toast.makeText(CursosActivity.this, "Não foi possível carregar os cursos.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Curso>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CursosActivity.this, "Sem conexão com o servidor. Tente novamente.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

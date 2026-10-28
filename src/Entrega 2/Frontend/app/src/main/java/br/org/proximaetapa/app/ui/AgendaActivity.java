package br.org.proximaetapa.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.model.AgendaItem;

/**
 * RFM05 — Agenda individual, agora lida do banco local (RFM15).
 *
 * Tem um botão fixo de check-in porque é desta tela que o aluno costuma sair
 * para registrar presença: ele está olhando o encontro de hoje.
 */
public class AgendaActivity extends AppCompatActivity {

    private AgendaAdapter adapter;
    private TextView txtVazio;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerAgenda;
    private Repositorio repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        repositorio = Repositorio.obter(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerAgenda = findViewById(R.id.recyclerAgenda);
        txtVazio = findViewById(R.id.txtVazio);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        adapter = new AgendaAdapter();
        recyclerAgenda.setLayoutManager(new LinearLayoutManager(this));
        recyclerAgenda.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::sincronizar);

        ExtendedFloatingActionButton btnCheckin = findViewById(R.id.btnCheckin);
        btnCheckin.setOnClickListener(v -> startActivity(new Intent(this, CheckinActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta do check-in: a agenda precisa refletir a presença recém-marcada.
        carregarDoBancoLocal();
    }

    private void carregarDoBancoLocal() {
        repositorio.listarAgenda(new Repositorio.Callback<List<AgendaItem>>() {
            @Override
            public void sucesso(List<AgendaItem> eventos) {
                adapter.atualizar(eventos);
                boolean vazio = eventos.isEmpty();
                txtVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
                recyclerAgenda.setVisibility(vazio ? View.GONE : View.VISIBLE);
            }

            @Override
            public void falha(String mensagem) {
                txtVazio.setVisibility(View.VISIBLE);
            }
        });
    }

    private void sincronizar() {
        repositorio.sincronizar(new Repositorio.Callback<Repositorio.ResultadoSincronizacao>() {
            @Override
            public void sucesso(Repositorio.ResultadoSincronizacao resultado) {
                swipeRefresh.setRefreshing(false);
                carregarDoBancoLocal();
            }

            @Override
            public void falha(String mensagem) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }
}

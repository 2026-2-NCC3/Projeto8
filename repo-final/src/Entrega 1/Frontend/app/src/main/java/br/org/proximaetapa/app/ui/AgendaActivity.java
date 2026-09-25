package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.model.AgendaItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** RFM05 - Agenda individual: cursos, encontros e demais atividades programadas. */
public class AgendaActivity extends AppCompatActivity {

    private AgendaAdapter adapter;
    private TextView txtVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerAgenda = findViewById(R.id.recyclerAgenda);
        txtVazio = findViewById(R.id.txtVazio);

        adapter = new AgendaAdapter();
        recyclerAgenda.setLayoutManager(new LinearLayoutManager(this));
        recyclerAgenda.setAdapter(adapter);

        carregarAgenda();
    }

    private void carregarAgenda() {
        ApiClient.get(this).listarAgenda().enqueue(new Callback<List<AgendaItem>>() {
            @Override
            public void onResponse(Call<List<AgendaItem>> call, Response<List<AgendaItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.atualizar(response.body());
                    txtVazio.setVisibility(adapter.estaVazio() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(AgendaActivity.this, "Não foi possível carregar a agenda.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AgendaItem>> call, Throwable t) {
                Toast.makeText(AgendaActivity.this, "Sem conexão com o servidor. Tente novamente.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

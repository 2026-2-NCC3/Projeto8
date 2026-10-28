package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.data.local.PresencaLocal;
import br.org.proximaetapa.app.util.Formato;

/**
 * RFM07 — Histórico de presença.
 *
 * Mostra as presenças já confirmadas pelo servidor e, acima delas, as que
 * ainda estão na fila esperando conexão. O aluno precisa enxergar essa
 * diferença: a presença pendente conta para ele, mas ainda não chegou à ONG.
 */
public class HistoricoActivity extends AppCompatActivity {

    private ItemSimplesAdapter adapter;
    private TextView txtVazio;
    private TextView txtResumo;
    private RecyclerView recycler;
    private Repositorio repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);

        repositorio = Repositorio.obter(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.titulo_historico);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerLista);
        txtVazio = findViewById(R.id.txtVazio);
        txtResumo = findViewById(R.id.txtResumo);
        txtVazio.setText(R.string.historico_vazio);

        adapter = new ItemSimplesAdapter(null);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        repositorio.listarHistoricoPresencas(new Repositorio.Callback<List<PresencaLocal>>() {
            @Override
            public void sucesso(List<PresencaLocal> presencas) {
                List<ItemSimplesAdapter.Item> itens = new ArrayList<>();
                for (PresencaLocal p : presencas) {
                    itens.add(new ItemSimplesAdapter.Item(
                            p.atividade,
                            p.curso,
                            Formato.dataHora(p.registradoEm),
                            getString(R.string.selo_confirmada),
                            false,
                            p));
                }
                adapter.atualizar(itens);

                boolean vazio = itens.isEmpty();
                txtVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
                recycler.setVisibility(vazio ? View.GONE : View.VISIBLE);

                txtResumo.setVisibility(View.VISIBLE);
                txtResumo.setText(getResources().getQuantityString(
                        R.plurals.total_presencas, presencas.size(), presencas.size()));
            }

            @Override
            public void falha(String mensagem) {
                txtVazio.setVisibility(View.VISIBLE);
            }
        });
    }
}

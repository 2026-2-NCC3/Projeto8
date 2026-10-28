package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.data.local.NotificacaoLocal;
import br.org.proximaetapa.app.util.Formato;

/**
 * RFM13 — Notificações.
 *
 * Tocar em uma notificação já a marca como lida no banco local; o servidor é
 * avisado em seguida, e se a chamada falhar a marcação local permanece — o
 * aluno não deve ver de novo como "não lida" algo que ele claramente leu.
 */
public class NotificacoesActivity extends AppCompatActivity {

    private ItemSimplesAdapter adapter;
    private TextView txtVazio;
    private RecyclerView recycler;
    private Repositorio repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);

        repositorio = Repositorio.obter(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.titulo_notificacoes);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerLista);
        txtVazio = findViewById(R.id.txtVazio);
        txtVazio.setText(R.string.notificacoes_vazio);

        adapter = new ItemSimplesAdapter(this::aoTocar);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        repositorio.listarNotificacoes(new Repositorio.Callback<List<NotificacaoLocal>>() {
            @Override
            public void sucesso(List<NotificacaoLocal> notificacoes) {
                List<ItemSimplesAdapter.Item> itens = new ArrayList<>();
                for (NotificacaoLocal n : notificacoes) {
                    itens.add(new ItemSimplesAdapter.Item(
                            n.titulo,
                            n.corpo,
                            Formato.dataHora(n.criadoEm),
                            rotuloDoTipo(n.tipo),
                            !n.lida,
                            n));
                }
                adapter.atualizar(itens);

                boolean vazio = itens.isEmpty();
                txtVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
                recycler.setVisibility(vazio ? View.GONE : View.VISIBLE);
            }

            @Override
            public void falha(String mensagem) {
                txtVazio.setVisibility(View.VISIBLE);
            }
        });
    }

    /** Traduz o tipo técnico que vem da API para algo que o aluno entende. */
    private String rotuloDoTipo(String tipo) {
        if (tipo == null) return null;
        switch (tipo) {
            case "curso_novo": return getString(R.string.tipo_curso_novo);
            case "agenda": return getString(R.string.tipo_agenda);
            case "certificado": return getString(R.string.tipo_certificado);
            default: return getString(R.string.tipo_mensagem);
        }
    }

    private void aoTocar(ItemSimplesAdapter.Item item) {
        NotificacaoLocal notificacao = (NotificacaoLocal) item.referencia;
        if (!notificacao.lida) {
            repositorio.marcarNotificacaoLida(notificacao.id);
            notificacao.lida = true;
            carregar();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notificacoes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_marcar_todas) {
            repositorio.marcarTodasLidas(this::carregar);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

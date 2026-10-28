package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.data.local.MensagemLocal;
import br.org.proximaetapa.app.util.Formato;

/** RFM11 — Mensagens e comunicados enviados pela organização. */
public class MensagensActivity extends AppCompatActivity {

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
        toolbar.setTitle(R.string.titulo_mensagens);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerLista);
        txtVazio = findViewById(R.id.txtVazio);
        txtVazio.setText(R.string.mensagens_vazio);

        adapter = new ItemSimplesAdapter(this::abrirMensagem);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        repositorio.listarMensagens(new Repositorio.Callback<List<MensagemLocal>>() {
            @Override
            public void sucesso(List<MensagemLocal> mensagens) {
                List<ItemSimplesAdapter.Item> itens = new ArrayList<>();
                for (MensagemLocal m : mensagens) {
                    itens.add(new ItemSimplesAdapter.Item(
                            m.titulo,
                            // Prévia curta: o texto inteiro aparece ao tocar.
                            resumir(m.corpo),
                            Formato.dataHora(m.criadoEm),
                            null,
                            false,
                            m));
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

    private String resumir(String texto) {
        if (texto == null) return "";
        return texto.length() > 90 ? texto.substring(0, 90) + "…" : texto;
    }

    private void abrirMensagem(ItemSimplesAdapter.Item item) {
        MensagemLocal mensagem = (MensagemLocal) item.referencia;
        new AlertDialog.Builder(this)
                .setTitle(mensagem.titulo)
                .setMessage(mensagem.corpo)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}

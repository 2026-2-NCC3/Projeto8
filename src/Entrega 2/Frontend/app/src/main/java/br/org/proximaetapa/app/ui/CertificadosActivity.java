package br.org.proximaetapa.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.data.local.CertificadoLocal;
import br.org.proximaetapa.app.util.Formato;

/**
 * RFM09 — Certificados.
 *
 * A lista abre offline (vem do banco local), mas abrir o PDF depende de
 * internet, porque o arquivo mora no servidor da ONG.
 */
public class CertificadosActivity extends AppCompatActivity {

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
        toolbar.setTitle(R.string.titulo_certificados);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerLista);
        txtVazio = findViewById(R.id.txtVazio);
        txtVazio.setText(R.string.certificados_vazio);

        adapter = new ItemSimplesAdapter(this::abrirCertificado);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        repositorio.listarCertificados(new Repositorio.Callback<List<CertificadoLocal>>() {
            @Override
            public void sucesso(List<CertificadoLocal> certificados) {
                List<ItemSimplesAdapter.Item> itens = new ArrayList<>();
                for (CertificadoLocal c : certificados) {
                    itens.add(new ItemSimplesAdapter.Item(
                            c.curso,
                            getString(R.string.carga_horaria_formato, c.cargaHoraria),
                            getString(R.string.emitido_em, Formato.data(c.emitidoEm)),
                            getString(R.string.selo_abrir),
                            false,
                            c));
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

    private void abrirCertificado(ItemSimplesAdapter.Item item) {
        CertificadoLocal certificado = (CertificadoLocal) item.referencia;
        if (certificado.certificadoUrl == null || certificado.certificadoUrl.isEmpty()) {
            Toast.makeText(this, R.string.certificado_sem_arquivo, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!repositorio.temConexao()) {
            Toast.makeText(this, R.string.certificado_precisa_internet, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(certificado.certificadoUrl)));
        } catch (Exception e) {
            // Nenhum navegador instalado é raro, mas não pode derrubar o app.
            Toast.makeText(this, R.string.erro_abrir_certificado, Toast.LENGTH_SHORT).show();
        }
    }
}

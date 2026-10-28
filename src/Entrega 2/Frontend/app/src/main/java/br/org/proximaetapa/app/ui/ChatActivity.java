package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.ApiClient;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.model.ChatMensagem;
import br.org.proximaetapa.app.model.EnvioChat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RFM12 — Canal de dúvidas com a equipe da Próxima Etapa.
 *
 * Diferente do resto do app, o chat não tem versão offline: uma pergunta
 * guardada no celular sem chegar a ninguém daria ao aluno a impressão falsa
 * de que foi atendido. Quando não há internet, a tela diz isso abertamente.
 */
public class ChatActivity extends AppCompatActivity {

    private ChatAdapter adapter;
    private RecyclerView recycler;
    private TextInputEditText edtMensagem;
    private ImageButton btnEnviar;
    private TextView txtVazio;
    private TextView txtAvisoOffline;
    private Repositorio repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        repositorio = Repositorio.obter(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerChat);
        edtMensagem = findViewById(R.id.edtMensagem);
        btnEnviar = findViewById(R.id.btnEnviar);
        txtVazio = findViewById(R.id.txtVazio);
        txtAvisoOffline = findViewById(R.id.txtAvisoOffline);

        adapter = new ChatAdapter();
        LinearLayoutManager gerenciador = new LinearLayoutManager(this);
        // Começa de baixo, como em qualquer aplicativo de mensagem: a fala
        // mais recente é a que importa.
        gerenciador.setStackFromEnd(true);
        recycler.setLayoutManager(gerenciador);
        recycler.setAdapter(adapter);

        btnEnviar.setOnClickListener(v -> enviar());

        carregar();
    }

    private void carregar() {
        if (!repositorio.temConexao()) {
            txtAvisoOffline.setVisibility(View.VISIBLE);
            txtVazio.setVisibility(adapter.tamanho() == 0 ? View.VISIBLE : View.GONE);
            btnEnviar.setEnabled(false);
            return;
        }

        txtAvisoOffline.setVisibility(View.GONE);
        btnEnviar.setEnabled(true);

        ApiClient.get(this).listarChat().enqueue(new Callback<List<ChatMensagem>>() {
            @Override
            public void onResponse(Call<List<ChatMensagem>> call, Response<List<ChatMensagem>> response) {
                List<ChatMensagem> mensagens = response.body();
                if (!response.isSuccessful() || mensagens == null) return;

                adapter.atualizar(mensagens);
                txtVazio.setVisibility(mensagens.isEmpty() ? View.VISIBLE : View.GONE);
                rolarParaOFim();
            }

            @Override
            public void onFailure(Call<List<ChatMensagem>> call, Throwable t) {
                txtAvisoOffline.setVisibility(View.VISIBLE);
            }
        });
    }

    private void enviar() {
        String texto = edtMensagem.getText() == null ? "" : edtMensagem.getText().toString().trim();
        if (texto.isEmpty()) return;

        if (!repositorio.temConexao()) {
            Toast.makeText(this, R.string.chat_precisa_internet, Toast.LENGTH_LONG).show();
            return;
        }

        // Mostra a mensagem na tela antes da resposta do servidor, para a
        // conversa parecer instantânea. Se o envio falhar, avisamos e a lista
        // é recarregada com o que realmente foi gravado.
        ChatMensagem provisoria = new ChatMensagem();
        provisoria.autor = "aluno";
        provisoria.texto = texto;
        provisoria.criado_em = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                new Locale("pt", "BR")).format(new Date());
        adapter.acrescentar(provisoria);
        txtVazio.setVisibility(View.GONE);
        rolarParaOFim();

        edtMensagem.setText("");
        btnEnviar.setEnabled(false);

        ApiClient.get(this).enviarChat(new EnvioChat(texto)).enqueue(new Callback<List<ChatMensagem>>() {
            @Override
            public void onResponse(Call<List<ChatMensagem>> call, Response<List<ChatMensagem>> response) {
                btnEnviar.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.atualizar(response.body());
                    rolarParaOFim();
                } else {
                    Toast.makeText(ChatActivity.this, R.string.erro_enviar_mensagem, Toast.LENGTH_SHORT).show();
                    carregar();
                }
            }

            @Override
            public void onFailure(Call<List<ChatMensagem>> call, Throwable t) {
                btnEnviar.setEnabled(true);
                Toast.makeText(ChatActivity.this, R.string.erro_rede, Toast.LENGTH_SHORT).show();
                carregar();
            }
        });
    }

    private void rolarParaOFim() {
        if (adapter.getItemCount() > 0) {
            recycler.post(() -> recycler.smoothScrollToPosition(adapter.getItemCount() - 1));
        }
    }
}

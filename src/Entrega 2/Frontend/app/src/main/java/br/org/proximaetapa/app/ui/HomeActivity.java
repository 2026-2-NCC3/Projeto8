package br.org.proximaetapa.app.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.data.Repositorio;
import br.org.proximaetapa.app.data.SessionManager;
import br.org.proximaetapa.app.data.local.NotificacaoLocal;
import br.org.proximaetapa.app.model.AgendaItem;
import br.org.proximaetapa.app.util.Formato;
import br.org.proximaetapa.app.util.Notificador;

/**
 * RFM02 — Página inicial: resumo da jornada do aluno e atalhos para o resto
 * do app. É a tela que abre depois do login e o centro da navegação.
 *
 * Também é onde a sincronização acontece de verdade no dia a dia: ao abrir e
 * ao puxar a tela para baixo.
 */
public class HomeActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefresh;
    private TextView txtSaudacao;
    private TextView txtNivel;
    private TextView txtCursos;
    private TextView txtPresencas;
    private TextView txtNotificacoes;
    private TextView txtAvisoSincronizacao;
    private TextView txtSemEncontros;
    private RecyclerView recyclerProximos;
    private AgendaAdapter adapterProximos;

    private Repositorio repositorio;
    private SessionManager sessao;

    /**
     * Pedido de permissão de notificação (obrigatório no Android 13+).
     * Pedimos aqui, e não na tela de login, porque o aluno já entendeu o que
     * o app faz — pedir permissão antes disso costuma gerar recusa.
     */
    private final ActivityResultLauncher<String> pedirPermissaoNotificacao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                // Sem permissão o app segue normalmente: os avisos continuam
                // dentro da tela de notificações.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        repositorio = Repositorio.obter(this);
        sessao = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        txtSaudacao = findViewById(R.id.txtSaudacao);
        txtNivel = findViewById(R.id.txtNivel);
        txtCursos = findViewById(R.id.txtCursos);
        txtPresencas = findViewById(R.id.txtPresencas);
        txtNotificacoes = findViewById(R.id.txtNotificacoes);
        txtAvisoSincronizacao = findViewById(R.id.txtAvisoSincronizacao);
        txtSemEncontros = findViewById(R.id.txtSemEncontros);
        recyclerProximos = findViewById(R.id.recyclerProximos);

        adapterProximos = new AgendaAdapter();
        recyclerProximos.setLayoutManager(new LinearLayoutManager(this));
        recyclerProximos.setAdapter(adapterProximos);
        // A lista de próximos encontros é curta e vive dentro de uma tela que
        // já rola; desligar o scroll dela evita duas áreas roláveis brigando.
        recyclerProximos.setNestedScrollingEnabled(false);

        configurarAtalhos();

        swipeRefresh.setOnRefreshListener(this::sincronizar);

        txtSaudacao.setText(getString(R.string.saudacao, primeiroNome(sessao.getNomeAluno())));

        pedirPermissaoDeNotificacaoSeNecessario();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega do banco local toda vez que a tela volta ao topo, para
        // refletir o que mudou em outras telas (check-in, notificação lida).
        carregarDoBancoLocal();
        sincronizar();
    }

    private void configurarAtalhos() {
        abrirAoTocar(R.id.cardCursos, CursosActivity.class);
        abrirAoTocar(R.id.cardAgenda, AgendaActivity.class);
        abrirAoTocar(R.id.cardCheckin, CheckinActivity.class);
        abrirAoTocar(R.id.cardHistorico, HistoricoActivity.class);
        abrirAoTocar(R.id.cardCard, CardActivity.class);
        abrirAoTocar(R.id.cardCertificados, CertificadosActivity.class);
        abrirAoTocar(R.id.cardTestePerfil, TestePerfilActivity.class);
        abrirAoTocar(R.id.cardMensagens, MensagensActivity.class);
        abrirAoTocar(R.id.cardChat, ChatActivity.class);
        abrirAoTocar(R.id.cardNotificacoes, NotificacoesActivity.class);
        abrirAoTocar(R.id.cardPerfil, PerfilActivity.class);
    }

    private void abrirAoTocar(int idDoCard, Class<?> destino) {
        MaterialCardView card = findViewById(idDoCard);
        if (card != null) {
            card.setOnClickListener(v -> startActivity(new Intent(this, destino)));
        }
    }

    /** Preenche a tela com o que já está no celular — funciona sem internet. */
    private void carregarDoBancoLocal() {
        repositorio.resumoLocal(new Repositorio.Callback<int[]>() {
            @Override
            public void sucesso(int[] numeros) {
                txtCursos.setText(String.valueOf(numeros[0]));
                txtNotificacoes.setText(String.valueOf(numeros[1]));
                txtPresencas.setText(String.valueOf(numeros[3]));
                mostrarPendentes(numeros[2]);
            }

            @Override
            public void falha(String mensagem) {
                // Leitura local não falha na prática; se falhar, a tela
                // simplesmente mantém os números anteriores.
            }
        });

        repositorio.buscarAluno(new Repositorio.Callback<br.org.proximaetapa.app.data.local.AlunoLocal>() {
            @Override
            public void sucesso(br.org.proximaetapa.app.data.local.AlunoLocal aluno) {
                if (aluno != null) {
                    txtSaudacao.setText(getString(R.string.saudacao, primeiroNome(aluno.nome)));
                    txtNivel.setText(getString(R.string.nivel_e_pontos, aluno.nivel, aluno.pontos));
                    txtNivel.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void falha(String mensagem) {
            }
        });

        repositorio.listarProximosEncontros(3, new Repositorio.Callback<List<AgendaItem>>() {
            @Override
            public void sucesso(List<AgendaItem> encontros) {
                adapterProximos.atualizar(encontros);
                txtSemEncontros.setVisibility(encontros.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerProximos.setVisibility(encontros.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void falha(String mensagem) {
            }
        });
    }

    /** Aviso honesto sobre check-ins que ainda não chegaram ao servidor. */
    private void mostrarPendentes(int pendentes) {
        if (pendentes > 0) {
            txtAvisoSincronizacao.setVisibility(View.VISIBLE);
            txtAvisoSincronizacao.setText(getResources().getQuantityString(
                    R.plurals.presencas_pendentes, pendentes, pendentes));
        } else {
            txtAvisoSincronizacao.setVisibility(View.GONE);
        }
    }

    private void sincronizar() {
        if (!repositorio.temConexao()) {
            swipeRefresh.setRefreshing(false);
            txtAvisoSincronizacao.setVisibility(View.VISIBLE);
            txtAvisoSincronizacao.setText(R.string.modo_offline);
            return;
        }

        swipeRefresh.setRefreshing(true);
        repositorio.sincronizar(new Repositorio.Callback<Repositorio.ResultadoSincronizacao>() {
            @Override
            public void sucesso(Repositorio.ResultadoSincronizacao resultado) {
                swipeRefresh.setRefreshing(false);
                carregarDoBancoLocal();

                if (resultado.presencasEnviadas > 0) {
                    Toast.makeText(HomeActivity.this,
                            getResources().getQuantityString(R.plurals.presencas_enviadas,
                                    resultado.presencasEnviadas, resultado.presencasEnviadas),
                            Toast.LENGTH_LONG).show();
                }
                if (resultado.notificacoesNovas > 0) {
                    avisarNovidades();
                }
            }

            @Override
            public void falha(String mensagem) {
                swipeRefresh.setRefreshing(false);
                if (mensagem != null && mensagem.contains("sessão")) {
                    sairPorSessaoExpirada();
                }
            }
        });
    }

    /** Transforma em notificação do Android o que chegou na sincronização (RFM13). */
    private void avisarNovidades() {
        repositorio.listarNotificacoesNaoExibidas(new Repositorio.Callback<List<NotificacaoLocal>>() {
            @Override
            public void sucesso(List<NotificacaoLocal> novas) {
                List<Integer> exibidas = new ArrayList<>();
                for (NotificacaoLocal n : novas) {
                    Notificador.avisar(HomeActivity.this, n.id, n.titulo, n.corpo);
                    exibidas.add(n.id);
                }
                if (!exibidas.isEmpty()) {
                    repositorio.marcarNotificacoesComoExibidas(exibidas);
                }
            }

            @Override
            public void falha(String mensagem) {
            }
        });
    }

    private void pedirPermissaoDeNotificacaoSeNecessario() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !Notificador.podeNotificar(this)) {
            pedirPermissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private String primeiroNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.trim().isEmpty()) return "aluno";
        return nomeCompleto.trim().split("\\s+")[0];
    }

    // ----- Menu -------------------------------------------------------------

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
        repositorio.contarPendentes(new Repositorio.Callback<Integer>() {
            @Override
            public void sucesso(Integer pendentes) {
                String mensagem = pendentes > 0
                        // Avisa antes de perder presenças que ainda não subiram.
                        ? getResources().getQuantityString(R.plurals.sair_com_pendentes, pendentes, pendentes)
                        : getString(R.string.confirmar_sair);

                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle(R.string.sair)
                        .setMessage(mensagem)
                        .setPositiveButton(R.string.sair, (d, w) -> sair())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }

            @Override
            public void falha(String mensagem) {
                sair();
            }
        });
    }

    private void sair() {
        repositorio.limparTudo(() -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void sairPorSessaoExpirada() {
        Toast.makeText(this, R.string.sessao_expirada, Toast.LENGTH_LONG).show();
        sair();
    }
}

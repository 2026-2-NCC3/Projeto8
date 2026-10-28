package br.org.proximaetapa.app.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.org.proximaetapa.app.data.local.AgendaLocal;
import br.org.proximaetapa.app.data.local.AlunoLocal;
import br.org.proximaetapa.app.data.local.AppDatabase;
import br.org.proximaetapa.app.data.local.CertificadoLocal;
import br.org.proximaetapa.app.data.local.CursoLocal;
import br.org.proximaetapa.app.data.local.DadosDao;
import br.org.proximaetapa.app.data.local.MensagemLocal;
import br.org.proximaetapa.app.data.local.NotificacaoLocal;
import br.org.proximaetapa.app.data.local.PresencaLocal;
import br.org.proximaetapa.app.data.local.PresencaPendente;
import br.org.proximaetapa.app.model.AgendaItem;
import br.org.proximaetapa.app.model.Curso;
import br.org.proximaetapa.app.model.EnvioPresencas;
import br.org.proximaetapa.app.model.PresencaPendenteEnvio;
import br.org.proximaetapa.app.model.RespostaEnvioPresencas;
import br.org.proximaetapa.app.model.ResultadoEnvioPresenca;
import br.org.proximaetapa.app.model.SyncResposta;
import retrofit2.Response;

/**
 * Ponte entre o banco local (Room) e a API — o coração da Entrega 2.
 *
 * A regra de toda a camada é "offline primeiro": as telas SEMPRE leem do banco
 * do celular, nunca direto da rede. A rede serve só para atualizar esse banco
 * em segundo plano. É isso que faz o app abrir instantaneamente e continuar
 * funcionando no meio de um encontro sem sinal.
 *
 * As operações de banco e de rede rodam em um executor de uma thread só
 * (`executor`), e o resultado volta para a interface pela `principal`
 * (Handler do looper principal) — o Android proíbe acesso a disco e rede na
 * thread da interface.
 */
public class Repositorio {

    /** Resultado de uma operação assíncrona, entregue já na thread da interface. */
    public interface Callback<T> {
        void sucesso(T dados);

        void falha(String mensagem);
    }

    /** Resumo do que aconteceu em uma sincronização, para a tela mostrar ao aluno. */
    public static class ResultadoSincronizacao {
        public int presencasEnviadas;
        public int presencasPendentes;
        public int cursosAtualizados;
        public int notificacoesNovas;
        public boolean houveRede;
        public String mensagemErro;
    }

    private static volatile Repositorio instancia;

    private final Context contexto;
    private final DadosDao dao;
    private final SessionManager sessao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private Repositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.dao = AppDatabase.obter(contexto).dao();
        this.sessao = new SessionManager(contexto);
    }

    public static Repositorio obter(Context contexto) {
        if (instancia == null) {
            synchronized (Repositorio.class) {
                if (instancia == null) {
                    instancia = new Repositorio(contexto);
                }
            }
        }
        return instancia;
    }

    // ========================================================================
    // Conexão
    // ========================================================================

    /**
     * Diz se o aparelho tem uma rede capaz de chegar à internet.
     *
     * Checar antes de tentar evita que o aluno espere 30 segundos por um
     * timeout quando já se sabe que está sem sinal — a tela consegue dizer
     * "você está offline" na hora.
     */
    public boolean temConexao() {
        ConnectivityManager cm = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capacidades = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capacidades != null
                && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // ========================================================================
    // Sincronização (RFM15)
    // ========================================================================

    /**
     * Executa o ciclo completo de sincronização, nesta ordem:
     *
     *   1. SOBE a fila de check-ins feitos offline;
     *   2. BAIXA o que mudou no servidor;
     *   3. grava tudo no banco local;
     *   4. guarda o carimbo do servidor para a próxima vez.
     *
     * Subir antes de baixar não é detalhe: se baixássemos primeiro, a agenda
     * recém-recebida ainda não conheceria as presenças que estão na fila e o
     * aluno veria um encontro ao qual compareceu marcado como ausente.
     */
    public void sincronizar(Callback<ResultadoSincronizacao> callback) {
        executor.execute(() -> {
            ResultadoSincronizacao resultado = new ResultadoSincronizacao();

            if (!temConexao()) {
                resultado.houveRede = false;
                resultado.presencasPendentes = dao.contarPendentes();
                entregar(callback, resultado);
                return;
            }
            resultado.houveRede = true;

            try {
                // 1. Fila de presenças (upload)
                resultado.presencasEnviadas = enviarFilaPendentes();

                // 2. Download incremental
                String desde = sessao.getUltimaSincronizacao();
                Response<SyncResposta> resposta = ApiClient.get(contexto).sincronizar(desde).execute();

                if (resposta.code() == 401) {
                    resultado.mensagemErro = "Sua sessão expirou. Entre novamente.";
                    entregarFalha(callback, resultado.mensagemErro);
                    return;
                }
                if (!resposta.isSuccessful() || resposta.body() == null) {
                    resultado.mensagemErro = "O servidor não respondeu como esperado.";
                    entregarFalha(callback, resultado.mensagemErro);
                    return;
                }

                SyncResposta pacote = resposta.body();

                // 3. Grava no banco local
                int novas = salvarPacote(pacote);
                resultado.cursosAtualizados = pacote.cursos == null ? 0 : pacote.cursos.size();
                resultado.notificacoesNovas = novas;

                // 4. Carimbo para a próxima sincronização
                if (pacote.servidor_em != null) {
                    sessao.salvarUltimaSincronizacao(pacote.servidor_em);
                }

                resultado.presencasPendentes = dao.contarPendentes();
                entregar(callback, resultado);

            } catch (Exception e) {
                // Sem internet no meio do caminho, DNS falhando, servidor
                // hibernando: nada disso pode quebrar o app. O aluno continua
                // com os dados que já estavam no celular.
                resultado.mensagemErro = "Não foi possível sincronizar agora. Seus dados offline continuam disponíveis.";
                resultado.presencasPendentes = dao.contarPendentes();
                entregar(callback, resultado);
            }
        });
    }

    /** Grava o pacote recebido no banco local. Devolve quantas notificações são novas. */
    private int salvarPacote(SyncResposta pacote) {
        if (pacote.aluno != null) {
            dao.salvarAluno(AlunoLocal.de(pacote.aluno));
        }

        if (pacote.cursos != null && !pacote.cursos.isEmpty()) {
            List<CursoLocal> cursos = new ArrayList<>();
            for (Curso c : pacote.cursos) cursos.add(CursoLocal.de(c));
            dao.salvarCursos(cursos);
        }

        // As inscrições chegam separadas dos cursos; marcamos o vínculo na
        // própria linha do curso para a lista conseguir destacar "inscrito".
        if (pacote.inscricoes != null) {
            for (br.org.proximaetapa.app.model.Inscricao i : pacote.inscricoes) {
                dao.marcarInscricao(i.curso_id, i.status);
            }
        }

        if (pacote.agenda != null && !pacote.agenda.isEmpty()) {
            List<AgendaLocal> agenda = new ArrayList<>();
            for (AgendaItem a : pacote.agenda) agenda.add(AgendaLocal.de(a));
            dao.salvarAgenda(agenda);
        }

        // Presenças e certificados vêm sempre completos: limpamos e regravamos
        // para que um registro removido no servidor também suma aqui.
        if (pacote.presencas != null) {
            dao.limparPresencas();
            List<PresencaLocal> presencas = new ArrayList<>();
            for (br.org.proximaetapa.app.model.Presenca p : pacote.presencas) {
                presencas.add(PresencaLocal.de(p));
            }
            if (!presencas.isEmpty()) dao.salvarPresencas(presencas);
        }

        if (pacote.certificados != null) {
            dao.limparCertificados();
            List<CertificadoLocal> certificados = new ArrayList<>();
            for (br.org.proximaetapa.app.model.Certificado c : pacote.certificados) {
                certificados.add(CertificadoLocal.de(c));
            }
            if (!certificados.isEmpty()) dao.salvarCertificados(certificados);
        }

        if (pacote.mensagens != null && !pacote.mensagens.isEmpty()) {
            List<MensagemLocal> mensagens = new ArrayList<>();
            for (br.org.proximaetapa.app.model.Mensagem m : pacote.mensagens) {
                mensagens.add(MensagemLocal.de(m));
            }
            dao.salvarMensagens(mensagens);
        }

        int novas = 0;
        if (pacote.notificacoes != null && !pacote.notificacoes.isEmpty()) {
            // Preserva quais notificações o app já mostrou na barra de status,
            // senão toda sincronização reexibiria as mesmas.
            Set<Integer> jaExibidas = new HashSet<>(dao.idsJaExibidos());
            List<NotificacaoLocal> notificacoes = new ArrayList<>();
            for (br.org.proximaetapa.app.model.Notificacao n : pacote.notificacoes) {
                NotificacaoLocal local = NotificacaoLocal.de(n);
                local.exibida = jaExibidas.contains(n.id);
                if (!local.exibida) novas++;
                notificacoes.add(local);
            }
            dao.salvarNotificacoes(notificacoes);
        }
        return novas;
    }

    /**
     * Envia a fila de check-ins pendentes e limpa os que o servidor confirmou.
     *
     * Um item só sai da fila quando o servidor diz "sincronizado" ou
     * "duplicado". Erros definitivos (QR inválido, aluno não inscrito) também
     * saem — insistir neles seria tentar para sempre. Erros temporários ficam
     * para a próxima tentativa.
     */
    private int enviarFilaPendentes() {
        List<PresencaPendente> pendentes = dao.listarPendentes();
        if (pendentes.isEmpty()) return 0;

        List<PresencaPendenteEnvio> envio = new ArrayList<>();
        for (PresencaPendente p : pendentes) {
            envio.add(new PresencaPendenteEnvio(p.uuidLocal, p.qrCode, p.registradoEm));
        }

        try {
            Response<RespostaEnvioPresencas> resposta =
                    ApiClient.get(contexto).enviarPresencas(new EnvioPresencas(envio)).execute();

            if (!resposta.isSuccessful() || resposta.body() == null) return 0;

            int enviadas = 0;
            for (ResultadoEnvioPresenca r : resposta.body().resultados) {
                if (r.resolvido()) {
                    dao.removerPendente(r.uuid_local);
                    if ("sincronizado".equals(r.status)) enviadas++;
                } else if ("qr_invalido".equals(r.status) || "nao_inscrito".equals(r.status)) {
                    // Não adianta reenviar: o motivo da recusa não muda sozinho.
                    dao.removerPendente(r.uuid_local);
                } else {
                    dao.registrarFalhaDeEnvio(r.uuid_local, r.erro);
                }
            }
            return enviadas;
        } catch (Exception e) {
            return 0;
        }
    }

    // ========================================================================
    // Check-in por QR Code (RFM06)
    // ========================================================================

    /**
     * Registra um check-in. SEMPRE grava primeiro na fila local e só depois
     * tenta enviar — é o que garante que a presença não se perde quando o
     * aluno está sem sinal no local do encontro.
     *
     * O retorno diz se a presença já foi confirmada no servidor ou se ficou
     * na fila, para a tela ser honesta com o aluno sobre o que aconteceu.
     */
    public void registrarCheckin(String qrCode, Callback<String> callback) {
        executor.execute(() -> {
            if (qrCode == null || qrCode.trim().isEmpty()) {
                entregarFalha(callback, "Código inválido. Tente escanear novamente.");
                return;
            }

            // Evita duas leituras seguidas do mesmo código gerarem dois itens.
            if (dao.jaEnfileirado(qrCode.trim()) > 0) {
                entregar(callback, "Esse check-in já está na fila, aguardando conexão.");
                return;
            }

            String uuid = UUID.randomUUID().toString();
            String agora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("pt", "BR")).format(new Date());
            dao.enfileirarPresenca(new PresencaPendente(uuid, qrCode.trim(), agora, "Check-in por QR Code"));

            if (!temConexao()) {
                entregar(callback, "Presença registrada no celular. Vamos enviar assim que houver internet.");
                return;
            }

            // Com internet, tenta enviar na hora para o aluno receber a
            // confirmação definitiva ainda no encontro.
            try {
                // Collections.singletonList, e não List.of: List.of só existe
                // a partir do Android 11 (API 30) e o app precisa rodar desde
                // o Android 7 (minSdk 24), que é o que boa parte dos alunos usa.
                Response<RespostaEnvioPresencas> resposta = ApiClient.get(contexto)
                        .enviarPresencas(new EnvioPresencas(Collections.singletonList(
                                new PresencaPendenteEnvio(uuid, qrCode.trim(), agora))))
                        .execute();

                if (!resposta.isSuccessful() || resposta.body() == null
                        || resposta.body().resultados == null
                        || resposta.body().resultados.isEmpty()) {
                    entregar(callback, "Presença registrada no celular. Vamos enviar assim que der.");
                    return;
                }

                ResultadoEnvioPresenca r = resposta.body().resultados.get(0);
                switch (r.status) {
                    case "sincronizado":
                        dao.removerPendente(uuid);
                        if (r.atividade_id != null) dao.marcarPresencaNaAgenda(r.atividade_id);
                        entregar(callback, "Presença confirmada em " + r.atividade + ".");
                        break;
                    case "duplicado":
                        dao.removerPendente(uuid);
                        entregar(callback, "Sua presença neste encontro já estava registrada.");
                        break;
                    case "nao_inscrito":
                        dao.removerPendente(uuid);
                        entregarFalha(callback, "Você não está inscrito no curso deste encontro.");
                        break;
                    case "qr_invalido":
                        dao.removerPendente(uuid);
                        entregarFalha(callback, "QR Code inválido ou de um encontro que já passou.");
                        break;
                    default:
                        entregar(callback, "Presença registrada no celular. Vamos enviar assim que der.");
                }
            } catch (Exception e) {
                entregar(callback, "Presença registrada no celular. Vamos enviar assim que houver internet.");
            }
        });
    }

    // ========================================================================
    // Leituras locais — as telas sempre leem daqui
    // ========================================================================

    public void listarCursos(Callback<List<Curso>> callback) {
        executor.execute(() -> {
            List<Curso> cursos = new ArrayList<>();
            for (CursoLocal c : dao.listarCursos()) cursos.add(c.paraModelo());
            entregar(callback, cursos);
        });
    }

    public void listarAgenda(Callback<List<AgendaItem>> callback) {
        executor.execute(() -> {
            List<AgendaItem> agenda = new ArrayList<>();
            for (AgendaLocal a : dao.listarAgenda()) agenda.add(a.paraModelo());
            entregar(callback, agenda);
        });
    }

    public void listarProximosEncontros(int limite, Callback<List<AgendaItem>> callback) {
        executor.execute(() -> {
            String hoje = new SimpleDateFormat("yyyy-MM-dd", new Locale("pt", "BR")).format(new Date());
            List<AgendaItem> agenda = new ArrayList<>();
            for (AgendaLocal a : dao.listarProximos(hoje, limite)) agenda.add(a.paraModelo());
            entregar(callback, agenda);
        });
    }

    public void listarHistoricoPresencas(Callback<List<PresencaLocal>> callback) {
        executor.execute(() -> entregar(callback, dao.listarPresencas()));
    }

    public void listarCertificados(Callback<List<CertificadoLocal>> callback) {
        executor.execute(() -> entregar(callback, dao.listarCertificados()));
    }

    public void listarNotificacoes(Callback<List<NotificacaoLocal>> callback) {
        executor.execute(() -> entregar(callback, dao.listarNotificacoes()));
    }

    public void listarMensagens(Callback<List<MensagemLocal>> callback) {
        executor.execute(() -> entregar(callback, dao.listarMensagens()));
    }

    public void buscarAluno(Callback<AlunoLocal> callback) {
        executor.execute(() -> entregar(callback, dao.buscarAluno()));
    }

    /** Números do cabeçalho da tela inicial, todos vindos do banco local. */
    public void resumoLocal(Callback<int[]> callback) {
        executor.execute(() -> {
            int cursosInscritos = dao.listarMeusCursos().size();
            int naoLidas = dao.contarNaoLidas();
            int pendentes = dao.contarPendentes();
            int presencas = dao.contarPresencas();
            entregar(callback, new int[]{cursosInscritos, naoLidas, pendentes, presencas});
        });
    }

    public void contarPendentes(Callback<Integer> callback) {
        executor.execute(() -> entregar(callback, dao.contarPendentes()));
    }

    // ========================================================================
    // Escritas locais
    // ========================================================================

    public void marcarNotificacaoLida(int id) {
        executor.execute(() -> {
            dao.marcarComoLida(id);
            try {
                ApiClient.get(contexto).marcarNotificacaoLida(id).execute();
            } catch (Exception ignorada) {
                // A marcação local já valeu; o servidor se alinha na próxima
                // sincronização. Não vale incomodar o aluno com esse erro.
            }
        });
    }

    public void marcarTodasLidas(Runnable aoTerminar) {
        executor.execute(() -> {
            dao.marcarTodasComoLidas();
            try {
                ApiClient.get(contexto).marcarTodasLidas().execute();
            } catch (Exception ignorada) {
                // Mesmo caso acima.
            }
            principal.post(aoTerminar);
        });
    }

    /** Guarda quais notificações já foram exibidas na barra de status. */
    public void marcarNotificacoesComoExibidas(List<Integer> ids) {
        executor.execute(() -> {
            for (Integer id : ids) dao.marcarComoExibida(id);
        });
    }

    public void listarNotificacoesNaoExibidas(Callback<List<NotificacaoLocal>> callback) {
        executor.execute(() -> entregar(callback, dao.listarNaoExibidas()));
    }

    /** Apaga os dados locais no logout, para o aparelho não guardar nada do aluno anterior. */
    public void limparTudo(Runnable aoTerminar) {
        executor.execute(() -> {
            dao.apagarAluno();
            dao.apagarCursos();
            dao.apagarAgenda();
            dao.limparPresencas();
            dao.limparCertificados();
            dao.apagarNotificacoes();
            dao.apagarMensagens();
            dao.apagarPendentes();
            sessao.limpar();
            principal.post(aoTerminar);
        });
    }

    // ========================================================================
    // Utilitários de thread
    // ========================================================================

    private <T> void entregar(Callback<T> callback, T dados) {
        principal.post(() -> callback.sucesso(dados));
    }

    private <T> void entregarFalha(Callback<T> callback, String mensagem) {
        principal.post(() -> callback.falha(mensagem));
    }
}

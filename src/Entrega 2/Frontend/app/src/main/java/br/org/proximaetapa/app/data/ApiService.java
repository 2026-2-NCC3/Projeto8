package br.org.proximaetapa.app.data;

import java.util.List;

import br.org.proximaetapa.app.model.AgendaItem;
import br.org.proximaetapa.app.model.CardAluno;
import br.org.proximaetapa.app.model.Certificado;
import br.org.proximaetapa.app.model.ChatMensagem;
import br.org.proximaetapa.app.model.Curso;
import br.org.proximaetapa.app.model.EnvioChat;
import br.org.proximaetapa.app.model.EnvioPresencas;
import br.org.proximaetapa.app.model.LoginRequest;
import br.org.proximaetapa.app.model.LoginResponse;
import br.org.proximaetapa.app.model.Mensagem;
import br.org.proximaetapa.app.model.Notificacao;
import br.org.proximaetapa.app.model.Pergunta;
import br.org.proximaetapa.app.model.Presenca;
import br.org.proximaetapa.app.model.RespostaEnvioPresencas;
import br.org.proximaetapa.app.model.RespostasPerfil;
import br.org.proximaetapa.app.model.ResultadoPerfil;
import br.org.proximaetapa.app.model.ResumoHome;
import br.org.proximaetapa.app.model.SyncResposta;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Contrato da API consumido pelo app (ver Backend/API.md para a documentação completa). */
public interface ApiService {

    // ----- Autenticação (RFM01) ---------------------------------------------
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // ----- Sincronização (RFM15) --------------------------------------------
    /**
     * Baixa o estado do aluno. Sem `desde` vem o pacote completo; com `desde`
     * vem só o que mudou desde a última sincronização bem-sucedida.
     */
    @GET("sync")
    Call<SyncResposta> sincronizar(@Query("desde") String desde);

    /** Envia a fila de check-ins feitos offline. */
    @POST("sync/presencas")
    Call<RespostaEnvioPresencas> enviarPresencas(@Body EnvioPresencas envio);

    // ----- Cursos e agenda (RFM03, RFM05) ------------------------------------
    @GET("cursos")
    Call<List<Curso>> listarCursos();

    @GET("cursos/inscritos/meus")
    Call<List<Curso>> listarMeusCursos();

    @POST("cursos/{id}/inscricao")
    Call<Void> inscrever(@Path("id") int cursoId);

    @GET("agenda")
    Call<List<AgendaItem>> listarAgenda();

    // ----- Página inicial e perfil (RFM02, RFM08, RFM14) ---------------------
    @GET("home")
    Call<ResumoHome> resumoHome();

    @GET("card")
    Call<CardAluno> card();

    @GET("perfil")
    Call<br.org.proximaetapa.app.model.Aluno> perfil();

    @PATCH("perfil")
    Call<br.org.proximaetapa.app.model.Aluno> atualizarPerfil(@Body java.util.Map<String, String> campos);

    // ----- Presença (RFM06, RFM07) --------------------------------------------
    @GET("presencas/historico")
    Call<List<Presenca>> historicoPresencas();

    // ----- Certificados (RFM09) -----------------------------------------------
    @GET("certificados")
    Call<List<Certificado>> listarCertificados();

    // ----- Teste de perfil (RFM10) --------------------------------------------
    @GET("perfil-teste/perguntas")
    Call<List<Pergunta>> perguntasPerfil();

    @POST("perfil-teste/respostas")
    Call<ResultadoPerfil> responderPerfil(@Body RespostasPerfil respostas);

    @GET("perfil-teste/resultados")
    Call<List<ResultadoPerfil>> resultadosPerfil();

    // ----- Mensagens, chat e notificações (RFM11, RFM12, RFM13) ---------------
    @GET("mensagens")
    Call<List<Mensagem>> listarMensagens();

    @GET("notificacoes")
    Call<List<Notificacao>> listarNotificacoes();

    @PATCH("notificacoes/{id}/lida")
    Call<Void> marcarNotificacaoLida(@Path("id") int id);

    @PATCH("notificacoes/lidas/todas")
    Call<Void> marcarTodasLidas();

    @GET("chat")
    Call<List<ChatMensagem>> listarChat();

    @POST("chat")
    Call<List<ChatMensagem>> enviarChat(@Body EnvioChat envio);
}

package br.org.proximaetapa.app.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Acesso ao banco local (Room).
 *
 * Reunimos todas as consultas em um DAO só porque o app tem poucas tabelas e
 * elas são sempre lidas juntas pela mesma tela de sincronização — separar em
 * oito interfaces não traria organização, só arquivos a mais.
 *
 * O padrão de escrita é sempre "substituir": a sincronização recebe a versão
 * mais recente do servidor e sobrescreve a cópia local (OnConflictStrategy
 * .REPLACE). A única tabela que NÃO segue isso é `presencas_pendentes`, que é
 * escrita pelo próprio aluno e só pode ser apagada depois da confirmação do
 * servidor.
 */
@Dao
public interface DadosDao {

    // ----- Aluno ------------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvarAluno(AlunoLocal aluno);

    @Query("SELECT * FROM aluno LIMIT 1")
    AlunoLocal buscarAluno();

    // ----- Cursos -----------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvarCursos(List<CursoLocal> cursos);

    @Query("SELECT * FROM cursos ORDER BY dataInicio ASC")
    List<CursoLocal> listarCursos();

    @Query("SELECT * FROM cursos WHERE statusInscricao IS NOT NULL ORDER BY dataInicio ASC")
    List<CursoLocal> listarMeusCursos();

    @Query("UPDATE cursos SET statusInscricao = :status WHERE id = :cursoId")
    void marcarInscricao(int cursoId, String status);

    // ----- Agenda -----------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvarAgenda(List<AgendaLocal> agenda);

    @Query("SELECT * FROM agenda ORDER BY data ASC, horaInicio ASC")
    List<AgendaLocal> listarAgenda();

    @Query("SELECT * FROM agenda WHERE data >= :hoje ORDER BY data ASC, horaInicio ASC LIMIT :limite")
    List<AgendaLocal> listarProximos(String hoje, int limite);

    /** Marca presença na agenda local assim que o aluno faz o check-in, antes mesmo de sincronizar. */
    @Query("UPDATE agenda SET presencaConfirmada = 1 WHERE id = :atividadeId")
    void marcarPresencaNaAgenda(int atividadeId);

    // ----- Presenças confirmadas (histórico) --------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvarPresencas(List<PresencaLocal> presencas);

    @Query("DELETE FROM presencas")
    void limparPresencas();

    @Query("SELECT * FROM presencas ORDER BY registradoEm DESC")
    List<PresencaLocal> listarPresencas();

    @Query("SELECT COUNT(*) FROM presencas")
    int contarPresencas();

    // ----- Fila de check-ins offline ----------------------------------------
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long enfileirarPresenca(PresencaPendente pendente);

    /** Ordem de chegada (FIFO): o primeiro check-in feito é o primeiro enviado. */
    @Query("SELECT * FROM presencas_pendentes ORDER BY id ASC")
    List<PresencaPendente> listarPendentes();

    @Query("SELECT COUNT(*) FROM presencas_pendentes")
    int contarPendentes();

    @Query("SELECT COUNT(*) FROM presencas_pendentes WHERE qrCode = :qrCode")
    int jaEnfileirado(String qrCode);

    @Query("DELETE FROM presencas_pendentes WHERE uuidLocal = :uuidLocal")
    void removerPendente(String uuidLocal);

    @Query("UPDATE presencas_pendentes SET tentativas = tentativas + 1, ultimoErro = :erro WHERE uuidLocal = :uuidLocal")
    void registrarFalhaDeEnvio(String uuidLocal, String erro);

    // ----- Certificados ------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvarCertificados(List<CertificadoLocal> certificados);

    @Query("DELETE FROM certificados")
    void limparCertificados();

    @Query("SELECT * FROM certificados ORDER BY emitidoEm DESC")
    List<CertificadoLocal> listarCertificados();

    // ----- Notificações -------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvarNotificacoes(List<NotificacaoLocal> notificacoes);

    @Query("SELECT * FROM notificacoes ORDER BY criadoEm DESC")
    List<NotificacaoLocal> listarNotificacoes();

    @Query("SELECT * FROM notificacoes WHERE exibida = 0 ORDER BY criadoEm ASC")
    List<NotificacaoLocal> listarNaoExibidas();

    @Query("SELECT COUNT(*) FROM notificacoes WHERE lida = 0")
    int contarNaoLidas();

    @Query("UPDATE notificacoes SET exibida = 1 WHERE id = :id")
    void marcarComoExibida(int id);

    @Query("UPDATE notificacoes SET lida = 1 WHERE id = :id")
    void marcarComoLida(int id);

    @Query("UPDATE notificacoes SET lida = 1")
    void marcarTodasComoLidas();

    /**
     * Mantém o `exibida` que já estava gravado ao regravar a notificação vinda
     * da sincronização — senão o app avisaria de novo sobre algo já avisado.
     */
    @Query("SELECT id FROM notificacoes WHERE exibida = 1")
    List<Integer> idsJaExibidos();

    // ----- Mensagens -----------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvarMensagens(List<MensagemLocal> mensagens);

    @Query("SELECT * FROM mensagens ORDER BY criadoEm DESC")
    List<MensagemLocal> listarMensagens();

    // ----- Logout ---------------------------------------------------------------
    /**
     * Apaga os dados do aluno ao sair da conta, para que outro aluno que use o
     * mesmo aparelho não veja nada do anterior (requisito de privacidade do
     * projeto). A fila de pendentes também é descartada: ela pertence à sessão
     * de quem fez os check-ins.
     */
    @Query("DELETE FROM aluno")
    void apagarAluno();

    @Query("DELETE FROM cursos")
    void apagarCursos();

    @Query("DELETE FROM agenda")
    void apagarAgenda();

    @Query("DELETE FROM notificacoes")
    void apagarNotificacoes();

    @Query("DELETE FROM mensagens")
    void apagarMensagens();

    @Query("DELETE FROM presencas_pendentes")
    void apagarPendentes();
}

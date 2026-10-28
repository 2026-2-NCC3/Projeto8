package br.org.proximaetapa.poo.servico;

import br.org.proximaetapa.poo.modelo.Aluno;
import br.org.proximaetapa.poo.modelo.Atividade;
import br.org.proximaetapa.poo.modelo.Inscricao;
import br.org.proximaetapa.poo.modelo.Presenca;
import br.org.proximaetapa.poo.repositorio.ControleDePresenca;
import br.org.proximaetapa.poo.repositorio.FilaDeSincronizacao;
import br.org.proximaetapa.poo.repositorio.RepositorioAtividade;
import br.org.proximaetapa.poo.repositorio.RepositorioEmMemoria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Check-in por QR Code (RFM06/RFM07) e sincronização dos check-ins feitos
 * offline (RFM15).
 *
 * <p>Reproduz a mesma sequência de validações do endpoint
 * {@code POST /presencas/checkin} do backend:</p>
 * <ol>
 *   <li>QR Code existe? — busca O(1) no índice do {@link RepositorioAtividade};</li>
 *   <li>o aluno está inscrito no curso daquele encontro? — consulta O(1) no
 *       HashSet do {@link ServicoInscricao};</li>
 *   <li>a presença já foi registrada? — consulta O(1) no HashSet do
 *       {@link ControleDePresenca} (equivale ao 409 do backend).</li>
 * </ol>
 * As três checagens são O(1): o check-in não fica mais lento conforme o
 * histórico de presenças da ONG cresce.
 */
public class ServicoPresenca {

    /** Pontos de gamificação creditados por presença confirmada. */
    public static final int PONTOS_POR_PRESENCA = 20;

    private final RepositorioEmMemoria<Aluno> alunos;
    private final RepositorioAtividade atividades;
    private final ServicoInscricao servicoInscricao;
    private final ControleDePresenca controle;
    private final FilaDeSincronizacao fila = new FilaDeSincronizacao();

    public ServicoPresenca(RepositorioEmMemoria<Aluno> alunos,
                           RepositorioAtividade atividades,
                           ServicoInscricao servicoInscricao,
                           ControleDePresenca controle) {
        this.alunos = alunos;
        this.atividades = atividades;
        this.servicoInscricao = servicoInscricao;
        this.controle = controle;
    }

    /** Check-in com internet disponível (envio imediato). */
    public Resultado registrarCheckIn(int alunoId, String qrCode, LocalDateTime momento) {
        return registrarCheckIn(alunoId, qrCode, momento, false);
    }

    /**
     * Registra a presença a partir do QR Code lido.
     *
     * @param viaFilaOffline marca a presença como sincronizada depois da
     *                       reconexão (o registro guarda o horário real da
     *                       leitura, não o da sincronização).
     */
    public Resultado registrarCheckIn(int alunoId, String qrCode,
                                      LocalDateTime momento, boolean viaFilaOffline) {
        Optional<Atividade> atividade = atividades.localizarPorQrCode(qrCode);  // O(1)
        if (atividade.isEmpty()) {
            return Resultado.recusa("QR Code inválido ou expirado: " + qrCode);
        }
        Atividade encontro = atividade.get();

        if (!servicoInscricao.estaInscrito(alunoId, encontro.getCursoId())) {   // O(1)
            return Resultado.recusa("Aluno " + alunoId
                    + " não está inscrito no curso do encontro \"" + encontro.getTitulo() + "\".");
        }

        // REGRA: presença não pode duplicar no mesmo encontro (UNIQUE do banco).
        if (controle.jaRegistrou(alunoId, encontro.getId())) {                  // O(1)
            return Resultado.recusa("Presença já registrada para \""
                    + encontro.getTitulo() + "\" — leitura duplicada ignorada.");
        }

        Presenca presenca = new Presenca(controle.proximoId(), alunoId,
                encontro.getId(), momento, viaFilaOffline);
        if (!controle.registrar(presenca)) {
            return Resultado.recusa("Presença já registrada para \"" + encontro.getTitulo() + "\".");
        }

        // Efeitos colaterais da presença confirmada.
        servicoInscricao.localizar(alunoId, encontro.getCursoId()).ifPresent(Inscricao::iniciar);
        alunos.localizarPorId(alunoId).ifPresent(a -> a.adicionarPontos(PONTOS_POR_PRESENCA));

        String nome = alunos.localizarPorId(alunoId).map(Aluno::getNome).orElse("Aluno " + alunoId);
        return Resultado.sucesso(String.format("Presença de %s em \"%s\" registrada (+%d pts)%s.",
                nome, encontro.getTitulo(), PONTOS_POR_PRESENCA,
                viaFilaOffline ? " [sincronizada da fila offline]" : ""), presenca);
    }

    /** Sem internet: o check-in vai para o fim da fila local do celular. */
    public void enfileirarOffline(int alunoId, String qrCode, LocalDateTime lidoEm) {
        fila.enfileirar(new FilaDeSincronizacao.CheckInPendente(alunoId, qrCode, lidoEm));
    }

    /**
     * Conexão restabelecida: drena a fila em ordem FIFO e tenta registrar cada
     * check-in pendente. A fila cuida da ordem; este serviço cuida das regras.
     */
    public List<FilaDeSincronizacao.ResultadoEnvio> sincronizar() {
        return fila.drenar(pendente -> {
            Resultado resultado = registrarCheckIn(
                    pendente.alunoId(), pendente.qrCode(), pendente.lidoEm(), true);
            return new FilaDeSincronizacao.ResultadoEnvio(
                    pendente, resultado.ok(), resultado.mensagem());
        });
    }

    public FilaDeSincronizacao getFila() { return fila; }

    public ControleDePresenca getControle() { return controle; }

    /** Histórico de presenças do aluno (tela RFM07). */
    public List<Presenca> historicoDoAluno(int alunoId) {
        return controle.presencasDoAluno(alunoId);
    }
}

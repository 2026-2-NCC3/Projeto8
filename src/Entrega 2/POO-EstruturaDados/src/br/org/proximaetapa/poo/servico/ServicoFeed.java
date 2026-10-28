package br.org.proximaetapa.poo.servico;

import br.org.proximaetapa.poo.modelo.Aluno;
import br.org.proximaetapa.poo.modelo.Inscricao;
import br.org.proximaetapa.poo.modelo.ItemFeed;
import br.org.proximaetapa.poo.modelo.Mensagem;
import br.org.proximaetapa.poo.modelo.Notificacao;
import br.org.proximaetapa.poo.repositorio.Comparadores;
import br.org.proximaetapa.poo.repositorio.RepositorioEmMemoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mensagens e notificações (RFM11/RFM12) e o <b>feed unificado</b> da tela
 * inicial do app.
 *
 * <h2>Onde o polimorfismo aparece</h2>
 * O feed mistura dois tipos diferentes: comunicados gerais ({@link Mensagem})
 * e avisos individuais ({@link Notificacao}). Como as duas classes herdam de
 * {@link ItemFeed}, o feed é uma única {@code List<ItemFeed>}:
 * <ul>
 *   <li>ordenada por <b>um</b> comparador tipado em {@code ItemFeed};</li>
 *   <li>impressa chamando {@code item.linhaDoFeed()} — cada objeto responde com
 *       o rótulo e o formato da sua própria classe, sem nenhum
 *       {@code if (item instanceof ...)}.</li>
 * </ul>
 * Acrescentar um terceiro tipo de item no feed (por exemplo "CursoSugerido")
 * exigiria apenas criar outra subclasse de {@code ItemFeed}: este serviço não
 * mudaria.
 */
public class ServicoFeed {

    private final RepositorioEmMemoria<Aluno> alunos;
    private final RepositorioEmMemoria<Inscricao> inscricoes;
    private final RepositorioEmMemoria<Mensagem> mensagens;
    private final RepositorioEmMemoria<Notificacao> notificacoes;

    public ServicoFeed(RepositorioEmMemoria<Aluno> alunos,
                       RepositorioEmMemoria<Inscricao> inscricoes,
                       RepositorioEmMemoria<Mensagem> mensagens,
                       RepositorioEmMemoria<Notificacao> notificacoes) {
        this.alunos = alunos;
        this.inscricoes = inscricoes;
        this.mensagens = mensagens;
        this.notificacoes = notificacoes;
    }

    /** Comunicado geral: gera uma notificação para cada aluno cadastrado. */
    public Mensagem publicarParaTodos(String titulo, String corpo, LocalDateTime criadoEm) {
        Mensagem mensagem = Mensagem.paraTodos(mensagens.proximoId(), titulo, corpo, criadoEm);
        mensagens.cadastrar(mensagem);
        for (Aluno aluno : alunos.listarTodos()) {
            entregar(aluno.getId(), mensagem);
        }
        return mensagem;
    }

    /** Comunicado de turma: notifica somente quem tem inscrição ativa no curso. */
    public Mensagem publicarParaCurso(int cursoId, String titulo, String corpo,
                                      LocalDateTime criadoEm) {
        Mensagem mensagem = Mensagem.paraCurso(
                mensagens.proximoId(), titulo, corpo, cursoId, criadoEm);
        mensagens.cadastrar(mensagem);
        for (Inscricao inscricao : inscricoes.localizar(
                i -> i.getCursoId() == cursoId && i.estaAtiva())) {
            entregar(inscricao.getAlunoId(), mensagem);
        }
        return mensagem;
    }

    private void entregar(int alunoId, Mensagem mensagem) {
        notificacoes.cadastrar(
                Notificacao.deMensagem(notificacoes.proximoId(), alunoId, mensagem));
    }

    /** Aviso gerado pelo próprio sistema (agenda, curso novo, certificado). */
    public Notificacao notificar(int alunoId, Notificacao.Tipo tipo, String titulo, String corpo) {
        Notificacao notificacao = new Notificacao(notificacoes.proximoId(), alunoId, tipo,
                titulo, corpo, null, LocalDateTime.now());
        return notificacoes.cadastrar(notificacao);
    }

    /**
     * FEED UNIFICADO do aluno: suas notificações + os comunicados gerais,
     * do mais recente para o mais antigo.
     *
     * @return lista heterogênea tratada de forma uniforme (polimorfismo).
     */
    public List<ItemFeed> feedDoAluno(int alunoId) {
        List<ItemFeed> itens = new ArrayList<>();
        itens.addAll(notificacoes.localizar(n -> n.getAlunoId() == alunoId));
        itens.addAll(mensagens.localizar(Mensagem::ehParaTodos));
        itens.sort(Comparadores.FEED_MAIS_RECENTE_PRIMEIRO);
        return itens;
    }

    public List<Notificacao> naoLidas(int alunoId) {
        return notificacoes.localizar(n -> n.getAlunoId() == alunoId && !n.isLida());
    }

    public int marcarTodasComoLidas(int alunoId) {
        int marcadas = 0;
        for (Notificacao n : naoLidas(alunoId)) {
            if (n.marcarComoLida()) marcadas++;
        }
        return marcadas;
    }

    public List<Notificacao> notificacoesDoAluno(int alunoId) {
        return notificacoes.localizar(n -> n.getAlunoId() == alunoId);
    }
}

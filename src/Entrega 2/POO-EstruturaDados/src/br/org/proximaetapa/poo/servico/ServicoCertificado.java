package br.org.proximaetapa.poo.servico;

import br.org.proximaetapa.poo.modelo.Aluno;
import br.org.proximaetapa.poo.modelo.Atividade;
import br.org.proximaetapa.poo.modelo.Certificado;
import br.org.proximaetapa.poo.modelo.Curso;
import br.org.proximaetapa.poo.modelo.Inscricao;
import br.org.proximaetapa.poo.modelo.Notificacao;
import br.org.proximaetapa.poo.repositorio.ControleDePresenca;
import br.org.proximaetapa.poo.repositorio.RepositorioAtividade;
import br.org.proximaetapa.poo.repositorio.RepositorioEmMemoria;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Emissão de certificados (RFM09) pelo critério de frequência da ONG.
 *
 * <h2>Regra</h2>
 * O aluno recebe o certificado quando comparece a pelo menos
 * {@code Certificado.FREQUENCIA_MINIMA} (75%) dos encontros do curso. A conta
 * fica no modelo ({@link Certificado#emitir}); aqui o serviço só reúne os dados
 * das três entidades envolvidas (Atividade, Presença, Inscrição).
 *
 * <h2>Estrutura de dados</h2>
 * Os ids dos encontros do curso são coletados em um {@code Set<Integer>} e
 * repassados ao {@link ControleDePresenca}, que testa a pertinência de cada um
 * no seu HashSet de presenças em O(1). O cálculo da frequência fica O(k), com k
 * = número de encontros do curso — e não O(n) sobre o histórico inteiro de
 * presenças da ONG.
 */
public class ServicoCertificado {

    /** Pontos de gamificação por curso concluído com certificado. */
    public static final int PONTOS_POR_CERTIFICADO = 50;

    private final RepositorioEmMemoria<Aluno> alunos;
    private final RepositorioEmMemoria<Curso> cursos;
    private final RepositorioEmMemoria<Certificado> certificados;
    private final RepositorioAtividade atividades;
    private final ControleDePresenca controle;
    private final ServicoInscricao servicoInscricao;
    private final ServicoFeed servicoFeed;

    /** Espelha o UNIQUE(aluno_id, curso_id) da tabela certificados. */
    private final Set<String> chavesEmitidas = new HashSet<>();

    public ServicoCertificado(RepositorioEmMemoria<Aluno> alunos,
                              RepositorioEmMemoria<Curso> cursos,
                              RepositorioEmMemoria<Certificado> certificados,
                              RepositorioAtividade atividades,
                              ControleDePresenca controle,
                              ServicoInscricao servicoInscricao,
                              ServicoFeed servicoFeed) {
        this.alunos = alunos;
        this.cursos = cursos;
        this.certificados = certificados;
        this.atividades = atividades;
        this.controle = controle;
        this.servicoInscricao = servicoInscricao;
        this.servicoFeed = servicoFeed;
    }

    /** Ids dos encontros do curso, em um Set para teste de pertinência O(1). */
    private Set<Integer> encontrosDoCurso(int cursoId) {
        return atividades.localizar(a -> a.getCursoId() == cursoId)
                         .stream()
                         .map(Atividade::getId)
                         .collect(Collectors.toSet());
    }

    /** Frequência do aluno no curso, entre 0.0 e 1.0. */
    public double frequencia(int alunoId, int cursoId) {
        Set<Integer> encontros = encontrosDoCurso(cursoId);
        int presencas = controle.contarPresencas(alunoId, encontros);
        return Certificado.calcularFrequencia(presencas, encontros.size());
    }

    public int presencasNoCurso(int alunoId, int cursoId) {
        return controle.contarPresencas(alunoId, encontrosDoCurso(cursoId));
    }

    public int totalDeEncontros(int cursoId) {
        return atividades.totalDeEncontrosDoCurso(cursoId);
    }

    /**
     * Tenta emitir o certificado do aluno no curso.
     *
     * <p>Recusa quando: aluno/curso inexistente, aluno não inscrito, curso sem
     * certificado previsto, certificado já emitido, ou frequência abaixo de
     * 75%. Quando emite, conclui a inscrição, credita pontos e notifica o
     * aluno.</p>
     */
    public Resultado emitir(int alunoId, int cursoId) {
        Optional<Aluno> aluno = alunos.localizarPorId(alunoId);
        Optional<Curso> curso = cursos.localizarPorId(cursoId);
        if (aluno.isEmpty() || curso.isEmpty()) {
            return Resultado.recusa("Aluno ou curso inexistente.");
        }
        if (!servicoInscricao.estaInscrito(alunoId, cursoId)) {
            return Resultado.recusa(aluno.get().getNome()
                    + " não está inscrito em \"" + curso.get().getTitulo() + "\".");
        }
        if (!curso.get().isPossuiCertificado()) {
            return Resultado.recusa("O curso \"" + curso.get().getTitulo()
                    + "\" não prevê emissão de certificado.");
        }
        String chave = Certificado.chaveUnica(alunoId, cursoId);
        if (chavesEmitidas.contains(chave)) {
            return Resultado.recusa("Certificado de \"" + curso.get().getTitulo()
                    + "\" já foi emitido para " + aluno.get().getNome() + ".");
        }

        Set<Integer> encontros = encontrosDoCurso(cursoId);
        int presencas = controle.contarPresencas(alunoId, encontros);

        // REGRA DE NEGÓCIO: a fábrica devolve null quando a frequência não bate.
        Certificado certificado = Certificado.emitir(
                certificados.proximoId(), alunoId, cursoId, presencas, encontros.size());

        if (certificado == null) {
            return Resultado.recusa(String.format(
                    "%s tem %d/%d presenças (%.0f%%) em \"%s\" — mínimo exigido: %.0f%%.",
                    aluno.get().getNome(), presencas, encontros.size(),
                    Certificado.calcularFrequencia(presencas, encontros.size()) * 100,
                    curso.get().getTitulo(), Certificado.FREQUENCIA_MINIMA * 100));
        }

        certificados.cadastrar(certificado);
        chavesEmitidas.add(chave);
        servicoInscricao.localizar(alunoId, cursoId).ifPresent(Inscricao::concluir);
        aluno.get().adicionarPontos(PONTOS_POR_CERTIFICADO);
        servicoFeed.notificar(alunoId, Notificacao.Tipo.CERTIFICADO,
                "Certificado disponível",
                "Seu certificado de \"" + curso.get().getTitulo()
                        + "\" já está disponível para download.");

        return Resultado.sucesso(String.format(
                "Certificado emitido para %s em \"%s\" — %d/%d presenças (%.0f%%), +%d pts.",
                aluno.get().getNome(), curso.get().getTitulo(), presencas, encontros.size(),
                certificado.getFrequencia() * 100, PONTOS_POR_CERTIFICADO), certificado);
    }

    /** Certificados do aluno (tela RFM09). */
    public List<Certificado> certificadosDoAluno(int alunoId) {
        return certificados.localizar(c -> c.getAlunoId() == alunoId);
    }
}

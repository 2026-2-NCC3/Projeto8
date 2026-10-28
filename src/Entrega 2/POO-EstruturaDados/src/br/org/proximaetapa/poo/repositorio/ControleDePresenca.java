package br.org.proximaetapa.poo.repositorio;

import br.org.proximaetapa.poo.modelo.Presenca;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Guarda as presenças registradas e <b>impede duplicidade</b> de check-in no
 * mesmo encontro.
 *
 * <h2>Por que HashSet</h2>
 * O banco protege a regra com {@code UNIQUE(aluno_id, atividade_id)} na tabela
 * {@code presencas}. Em memória, a mesma proteção é feita por um
 * {@code HashSet<Presenca.Chave>}:
 * <ul>
 *   <li>{@code contains} e {@code add} em <b>O(1)</b> médio — a verificação
 *       acontece no instante em que o QR Code é lido, com a fila de alunos
 *       esperando na porta;</li>
 *   <li>alternativa sem Set seria varrer todas as presenças já registradas
 *       comparando aluno e atividade: <b>O(n)</b>, degradando conforme o
 *       histórico cresce (e o histórico só cresce);</li>
 *   <li>o elemento do Set é o {@code record Presenca.Chave}, que já tem
 *       {@code equals}/{@code hashCode} gerados sobre os dois campos — é
 *       justamente disso que o HashSet depende para reconhecer a duplicata.</li>
 * </ul>
 * O {@link RepositorioEmMemoria} interno continua guardando os registros
 * completos (para histórico, contagem e cálculo de frequência); o HashSet é
 * apenas o índice de unicidade, como um índice único no banco.
 */
public class ControleDePresenca {

    private final RepositorioEmMemoria<Presenca> presencas = new RepositorioEmMemoria<>();
    private final Set<Presenca.Chave> chavesRegistradas = new HashSet<>();

    /** Já existe presença deste aluno neste encontro? — O(1). */
    public boolean jaRegistrou(int alunoId, int atividadeId) {
        return chavesRegistradas.contains(new Presenca.Chave(alunoId, atividadeId));
    }

    /**
     * REGRA DE NEGÓCIO: registra a presença apenas se ela ainda não existir.
     *
     * @return {@code true} se registrou; {@code false} se era duplicata — e
     *         nesse caso nada é gravado (o backend responde 409).
     */
    public boolean registrar(Presenca presenca) {
        if (!chavesRegistradas.add(presenca.chaveUnica())) {
            return false; // add devolve false quando a chave já estava no Set
        }
        presencas.cadastrar(presenca);
        return true;
    }

    /** Id a usar na próxima presença (equivale ao AUTOINCREMENT). */
    public int proximoId() {
        return presencas.proximoId();
    }

    /** Histórico completo de presenças de um aluno — O(n). */
    public List<Presenca> presencasDoAluno(int alunoId) {
        return presencas.localizar(p -> p.getAlunoId() == alunoId);
    }

    /** Quantos alunos compareceram a um encontro. */
    public List<Presenca> presencasDaAtividade(int atividadeId) {
        return presencas.localizar(p -> p.getAtividadeId() == atividadeId);
    }

    /**
     * Quantas presenças o aluno tem entre as atividades informadas.
     * Usado pelo cálculo de frequência do certificado: recebe os ids dos
     * encontros do curso em um {@link Set} para que cada teste de pertinência
     * seja O(1), em vez de {@code List.contains}, que seria O(n).
     */
    public int contarPresencas(int alunoId, Set<Integer> atividadesDoCurso) {
        int total = 0;
        for (Integer atividadeId : atividadesDoCurso) {
            if (jaRegistrou(alunoId, atividadeId)) total++;
        }
        return total;
    }

    public List<Presenca> listarTodas() {
        return presencas.listarTodos();
    }

    public int total() {
        return presencas.total();
    }
}

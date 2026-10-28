package br.org.proximaetapa.poo.repositorio;

import br.org.proximaetapa.poo.modelo.Atividade;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório especializado de {@link Atividade}.
 *
 * <p><b>HERANÇA aplicada à estrutura de dados:</b> herda de
 * {@link RepositorioEmMemoria} toda a mecânica de lista + índice por id e
 * acrescenta <b>um segundo índice</b>: {@code HashMap<String, Atividade>} com o
 * QR Code como chave.</p>
 *
 * <p><b>Motivo de domínio:</b> o check-in do app não conhece o id da atividade —
 * o leitor devolve apenas a string do QR Code ({@code POST /presencas/checkin}
 * recebe {@code qr_code}). Sem esse índice, cada leitura exigiria varrer todas
 * as atividades de todos os cursos comparando strings (O(n)); com ele, a
 * atividade é encontrada em O(1), o que importa porque o check-in acontece com
 * uma turma inteira passando o celular ao mesmo tempo na porta da oficina.
 * É exatamente o papel do {@code UNIQUE(qr_code)} no banco.</p>
 *
 * <p>Os métodos {@code cadastrar} e {@code remover} são sobrescritos
 * (POLIMORFISMO por sobrescrita) para manter os dois índices sempre
 * sincronizados: a chamada a {@code super} faz o trabalho comum e o restante
 * cuida do índice extra.</p>
 */
public class RepositorioAtividade extends RepositorioEmMemoria<Atividade> {

    /** Índice secundário: QR Code -> atividade. */
    private final Map<String, Atividade> indicePorQrCode = new HashMap<>();

    @Override
    public Atividade cadastrar(Atividade atividade) {
        if (indicePorQrCode.containsKey(atividade.getQrCode())) {
            throw new IllegalArgumentException(
                    "QR Code já utilizado por outra atividade: " + atividade.getQrCode());
        }
        super.cadastrar(atividade);                // lista + índice por id
        indicePorQrCode.put(atividade.getQrCode(), atividade);
        return atividade;
    }

    @Override
    public boolean remover(int id) {
        Optional<Atividade> atividade = localizarPorId(id);
        boolean removido = super.remover(id);
        if (removido) {
            atividade.ifPresent(a -> indicePorQrCode.remove(a.getQrCode()));
        }
        return removido;
    }

    /** LOCALIZAR PELO QR CODE — O(1), é a operação central do check-in. */
    public Optional<Atividade> localizarPorQrCode(String qrCode) {
        if (qrCode == null) return Optional.empty();
        return Optional.ofNullable(indicePorQrCode.get(qrCode));
    }

    /** Quantos encontros um curso possui (base do cálculo de frequência). */
    public int totalDeEncontrosDoCurso(int cursoId) {
        return localizar(a -> a.getCursoId() == cursoId).size();
    }
}

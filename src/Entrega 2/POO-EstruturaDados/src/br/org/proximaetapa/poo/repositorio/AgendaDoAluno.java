package br.org.proximaetapa.poo.repositorio;

import br.org.proximaetapa.poo.modelo.Atividade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Agenda de encontros de um aluno (tela "Minha agenda", RFM05).
 *
 * <h2>Por que TreeMap e não ArrayList</h2>
 * A agenda tem duas exigências que vêm do uso real do app:
 * <ol>
 *   <li>Sair <b>sempre</b> em ordem cronológica, mesmo quando a ONG cadastra um
 *       encontro de reposição fora de ordem.</li>
 *   <li>Responder a perguntas de <b>intervalo</b>: "quais são os próximos
 *       encontros a partir de agora?", "o que tenho nesta semana?".</li>
 * </ol>
 *
 * <p>Com {@code ArrayList} seria preciso reordenar a lista
 * ({@code O(n log n)}) a cada inserção, ou ordenar na hora de exibir, e uma
 * consulta de intervalo exigiria varrer tudo ({@code O(n)}).</p>
 *
 * <p>O {@code TreeMap<LocalDateTime, List<Atividade>>} é uma árvore
 * balanceada ordenada pela chave: a inserção já deixa o encontro na posição
 * cronológica correta em {@code O(log n)}, iterar devolve tudo em ordem em
 * {@code O(n)} sem ordenar nada, e as consultas de intervalo usam
 * {@code tailMap} / {@code headMap} / {@code subMap}, que posicionam na árvore
 * em {@code O(log n)} e percorrem apenas a faixa pedida — não a agenda
 * inteira.</p>
 *
 * <p>O valor é uma {@code List<Atividade>} porque dois cursos diferentes podem
 * ter encontros marcados no mesmo dia e horário (conflito de agenda que o app
 * precisa mostrar, não esconder).</p>
 */
public class AgendaDoAluno {

    private final int alunoId;
    private final NavigableMap<LocalDateTime, List<Atividade>> encontros = new TreeMap<>();

    public AgendaDoAluno(int alunoId) {
        this.alunoId = alunoId;
    }

    public int getAlunoId() { return alunoId; }

    /** Insere o encontro já na posição cronológica correta — O(log n). */
    public void adicionar(Atividade atividade) {
        encontros.computeIfAbsent(atividade.getDataHoraInicio(), k -> new ArrayList<>())
                 .add(atividade);
    }

    /** Remove um encontro específico (curso cancelado, aluno desistente). */
    public boolean remover(Atividade atividade) {
        LocalDateTime chave = atividade.getDataHoraInicio();
        List<Atividade> doHorario = encontros.get(chave);
        if (doHorario == null) return false;
        boolean removido = doHorario.remove(atividade);
        if (doHorario.isEmpty()) encontros.remove(chave);
        return removido;
    }

    /** Toda a agenda em ordem cronológica — O(n), sem precisar ordenar. */
    public List<Atividade> emOrdemCronologica() {
        return achatar(encontros);
    }

    /**
     * PRÓXIMOS ENCONTROS a partir de um instante — consulta de intervalo.
     * {@code tailMap(referencia, true)} devolve uma visão da subárvore a partir
     * da chave: O(log n) para posicionar + O(k) para os k encontros retornados.
     */
    public List<Atividade> proximosEncontros(LocalDateTime referencia) {
        return achatar(encontros.tailMap(referencia, true));
    }

    /** Os {@code quantidade} primeiros encontros futuros (cartão da tela inicial). */
    public List<Atividade> proximosEncontros(LocalDateTime referencia, int quantidade) {
        List<Atividade> proximos = proximosEncontros(referencia);
        return proximos.subList(0, Math.min(quantidade, proximos.size()));
    }

    /** HISTÓRICO: encontros já realizados — {@code headMap}, O(log n) + O(k). */
    public List<Atividade> encontrosAnteriores(LocalDateTime referencia) {
        return achatar(encontros.headMap(referencia, false));
    }

    /**
     * Encontros dentro de uma faixa de datas, inclusive nas duas pontas
     * (usado na visão semanal/mensal da agenda) — {@code subMap}.
     */
    public List<Atividade> encontrosEntre(LocalDate inicio, LocalDate fim) {
        LocalDateTime de = inicio.atStartOfDay();
        LocalDateTime ate = fim.atTime(23, 59, 59);
        return achatar(encontros.subMap(de, true, ate, true));
    }

    /** O próximo compromisso do aluno — O(log n). */
    public Optional<Atividade> proximoEncontro(LocalDateTime referencia) {
        Map.Entry<LocalDateTime, List<Atividade>> entrada = encontros.ceilingEntry(referencia);
        if (entrada == null || entrada.getValue().isEmpty()) return Optional.empty();
        return Optional.of(entrada.getValue().get(0));
    }

    /** O primeiro e o último encontro da agenda — O(1) na árvore. */
    public Optional<Atividade> primeiroEncontro() {
        return encontros.isEmpty() ? Optional.empty()
                : Optional.of(encontros.firstEntry().getValue().get(0));
    }

    public Optional<Atividade> ultimoEncontro() {
        if (encontros.isEmpty()) return Optional.empty();
        List<Atividade> ultimos = encontros.lastEntry().getValue();
        return Optional.of(ultimos.get(ultimos.size() - 1));
    }

    public int total() {
        int soma = 0;
        for (List<Atividade> lista : encontros.values()) soma += lista.size();
        return soma;
    }

    /** Transforma a visão do mapa em uma lista plana, preservando a ordem. */
    private List<Atividade> achatar(Map<LocalDateTime, List<Atividade>> visao) {
        List<Atividade> saida = new ArrayList<>();
        for (List<Atividade> doHorario : visao.values()) {
            saida.addAll(doHorario);
        }
        return Collections.unmodifiableList(saida);
    }
}

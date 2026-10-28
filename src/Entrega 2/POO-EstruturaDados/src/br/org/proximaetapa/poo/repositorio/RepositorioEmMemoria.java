package br.org.proximaetapa.poo.repositorio;

import br.org.proximaetapa.poo.modelo.Identificavel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Repositório genérico em memória — base de todos os repositórios do protótipo.
 *
 * <h2>Estruturas de dados e por quê</h2>
 * <ul>
 *   <li><b>{@code ArrayList<T>}</b> — preserva a ordem de inserção e dá acesso
 *       sequencial rápido para listar, filtrar e ordenar. Percorrer é O(n).</li>
 *   <li><b>{@code HashMap<Integer, T>}</b> — índice pelo id. Localizar, editar
 *       e remover por id ficam em O(1) médio, em vez de O(n) varrendo a lista.
 *       É o mesmo papel do índice de chave primária no banco.</li>
 * </ul>
 * As duas estruturas são mantidas sincronizadas em toda escrita: é um custo de
 * memória extra (as referências aparecem nas duas) em troca de leitura rápida.
 *
 * <h2>Evolução em relação à Entrega 1</h2>
 * Na Entrega 1 o construtor exigia uma {@code Function<T, Integer>} para
 * extrair o id. Agora o tipo é limitado a {@code T extends Identificavel}
 * (POLIMORFISMO de interface): o repositório chama {@code item.getId()}
 * diretamente, o construtor ficou sem parâmetros e passou a ser impossível
 * instanciar o repositório com um extrator de id errado.
 *
 * @param <T> entidade de domínio que possui id próprio
 */
public class RepositorioEmMemoria<T extends Identificavel> {

    /** Ordem de inserção. */
    private final List<T> itens = new ArrayList<>();

    /** Índice por id para acesso O(1). */
    private final Map<Integer, T> indicePorId = new HashMap<>();

    /** Gerador de id sequencial, no lugar do AUTOINCREMENT do banco. */
    private int ultimoId = 0;

    /** Próximo id disponível (equivale ao AUTOINCREMENT da tabela). */
    public int proximoId() {
        return ++ultimoId;
    }

    /**
     * CADASTRAR — O(1) amortizado.
     *
     * @throws IllegalArgumentException se o id já existir (espelha a violação
     *         de chave primária do banco).
     */
    public T cadastrar(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Não é possível cadastrar um item nulo.");
        }
        int id = item.getId();
        if (indicePorId.containsKey(id)) {
            throw new IllegalArgumentException("Já existe um registro com id " + id);
        }
        itens.add(item);
        indicePorId.put(id, item);
        if (id > ultimoId) ultimoId = id;
        return item;
    }

    /** LOCALIZAR POR ID — O(1) médio pelo HashMap. */
    public Optional<T> localizarPorId(int id) {
        return Optional.ofNullable(indicePorId.get(id));
    }

    /** BUSCAR POR FILTRO — O(n); equivale a um WHERE sem índice. */
    public List<T> localizar(Predicate<T> filtro) {
        List<T> resultado = new ArrayList<>();
        for (T item : itens) {
            if (filtro.test(item)) resultado.add(item);
        }
        return resultado;
    }

    /** Primeiro item que satisfaz o filtro, se existir — O(n). */
    public Optional<T> localizarPrimeiro(Predicate<T> filtro) {
        for (T item : itens) {
            if (filtro.test(item)) return Optional.of(item);
        }
        return Optional.empty();
    }

    /**
     * EDITAR — substitui o registro mantendo sua posição na lista.
     * Localizar o item é O(1) pelo índice; a troca na lista é O(n) por causa do
     * {@code indexOf}, custo aceitável no volume do protótipo.
     *
     * @return {@code false} se não existir registro com esse id.
     */
    public boolean editar(int id, T novoItem) {
        T antigo = indicePorId.get(id);
        if (antigo == null) return false;
        if (novoItem.getId() != id) {
            throw new IllegalArgumentException("O id do registro não pode ser alterado na edição.");
        }
        itens.set(itens.indexOf(antigo), novoItem);
        indicePorId.put(id, novoItem);
        return true;
    }

    /** REMOVER por id — O(n) por causa da remoção na lista. */
    public boolean remover(int id) {
        T item = indicePorId.remove(id);
        if (item == null) return false;
        return itens.remove(item);
    }

    /** VISUALIZAR todos, na ordem de inserção (lista somente leitura). */
    public List<T> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(itens));
    }

    /**
     * ORDENAR por qualquer critério — O(n log n).
     * Usa uma cópia: a ordem de inserção original é preservada.
     */
    public List<T> listarOrdenado(Comparator<T> comparador) {
        List<T> copia = new ArrayList<>(itens);
        copia.sort(comparador);
        return copia;
    }

    /** Ordena e corta os N primeiros (usado no ranking de alunos). */
    public List<T> listarPrimeiros(Comparator<T> comparador, int quantidade) {
        List<T> ordenado = listarOrdenado(comparador);
        return ordenado.subList(0, Math.min(quantidade, ordenado.size()));
    }

    public boolean existe(int id) {
        return indicePorId.containsKey(id);
    }

    public int total() {
        return itens.size();
    }

    public boolean estaVazio() {
        return itens.isEmpty();
    }
}

package br.org.proximaetapa.poo.repositorio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Repositório genérico em memória, base de todos os repositórios do
 * protótipo (Aluno, Curso, Atividade, Inscrição, Presença).
 *
 * Estruturas de dados usadas e por quê:
 *  - ArrayList<T>: mantém a ordem de inserção e permite iteração
 *    (listar, filtrar) em O(n), adequada para volumes pequenos/médios.
 *  - HashMap<Integer, T>: índice por id para localizar/editar/remover em
 *    tempo médio O(1), evitando varrer a lista inteira a cada operação.
 * As duas estruturas são mantidas sincronizadas em toda operação de escrita.
 */
public class RepositorioEmMemoria<T> {

    private final List<T> itens = new ArrayList<>();
    private final Map<Integer, T> indicePorId = new HashMap<>();
    private final Function<T, Integer> extratorId;

    public RepositorioEmMemoria(Function<T, Integer> extratorId) {
        this.extratorId = extratorId;
    }

    /** Cadastrar */
    public T cadastrar(T item) {
        int id = extratorId.apply(item);
        if (indicePorId.containsKey(id)) {
            throw new IllegalArgumentException("Já existe um registro com id " + id);
        }
        itens.add(item);
        indicePorId.put(id, item);
        return item;
    }

    /** Localizar por id — O(1) via HashMap */
    public Optional<T> localizarPorId(int id) {
        return Optional.ofNullable(indicePorId.get(id));
    }

    /** Localizar — busca por predicado (equivalente a um filtro/"WHERE") */
    public List<T> localizar(Predicate<T> filtro) {
        List<T> resultado = new ArrayList<>();
        for (T item : itens) {
            if (filtro.test(item)) resultado.add(item);
        }
        return resultado;
    }

    /** Editar: substitui o item pelo id, mantendo a posição na lista */
    public boolean editar(int id, T novoItem) {
        if (!indicePorId.containsKey(id)) return false;
        T antigo = indicePorId.get(id);
        int posicao = itens.indexOf(antigo);
        itens.set(posicao, novoItem);
        indicePorId.put(id, novoItem);
        return true;
    }

    /** Remover por id */
    public boolean remover(int id) {
        T item = indicePorId.remove(id);
        if (item == null) return false;
        return itens.remove(item);
    }

    /** Visualizar todos (cópia defensiva) */
    public List<T> listarTodos() {
        return new ArrayList<>(itens);
    }

    /** Ordenar por um comparador qualquer (ex.: por nome, por data, por pontos) */
    public List<T> listarOrdenado(Comparator<T> comparador) {
        List<T> copia = new ArrayList<>(itens);
        copia.sort(comparador);
        return copia;
    }

    public int total() {
        return itens.size();
    }
}

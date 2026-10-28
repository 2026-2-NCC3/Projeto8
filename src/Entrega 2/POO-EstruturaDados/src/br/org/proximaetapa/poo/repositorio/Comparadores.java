package br.org.proximaetapa.poo.repositorio;

import br.org.proximaetapa.poo.modelo.Aluno;
import br.org.proximaetapa.poo.modelo.Atividade;
import br.org.proximaetapa.poo.modelo.Curso;
import br.org.proximaetapa.poo.modelo.ItemFeed;

import java.util.Comparator;

/**
 * Critérios de ordenação reutilizados pelas telas do app.
 *
 * <p><b>POLIMORFISMO por composição de funções:</b> o
 * {@link RepositorioEmMemoria#listarOrdenado(Comparator)} não conhece nenhuma
 * entidade concreta — recebe um {@link Comparator} e aplica. Trocar o critério
 * de ordenação (ranking por pontos, lista por nome, cursos por data) não exige
 * tocar no repositório, apenas passar outro comparador. Centralizar os
 * comparadores aqui evita que a mesma regra de ordenação seja reescrita em cada
 * tela com um resultado ligeiramente diferente.</p>
 *
 * <p>Ordenar é O(n log n) ({@code List.sort} usa TimSort).</p>
 */
public final class Comparadores {

    private Comparadores() { } // classe utilitária: não deve ser instanciada

    /** Ranking de gamificação: mais pontos primeiro; empate resolvido pelo nome. */
    public static final Comparator<Aluno> ALUNO_POR_PONTOS_DESC =
            Comparator.comparingInt(Aluno::getPontos).reversed()
                      .thenComparing(Aluno::getNome);

    /** Lista de alunos em ordem alfabética (relatório da coordenação). */
    public static final Comparator<Aluno> ALUNO_POR_NOME =
            Comparator.comparing(Aluno::getNome, String.CASE_INSENSITIVE_ORDER);

    /** Vitrine de cursos: os que começam primeiro aparecem no topo. */
    public static final Comparator<Curso> CURSO_POR_DATA_INICIO =
            Comparator.comparing(Curso::getDataInicio)
                      .thenComparing(Curso::getTitulo);

    /** Cursos com mais vagas livres primeiro (destaque "ainda dá tempo"). */
    public static final Comparator<Curso> CURSO_POR_VAGAS_DESC =
            Comparator.comparingInt(Curso::getVagasDisponiveis).reversed()
                      .thenComparing(Curso::getTitulo);

    public static final Comparator<Curso> CURSO_POR_TITULO =
            Comparator.comparing(Curso::getTitulo, String.CASE_INSENSITIVE_ORDER);

    /** Encontros em ordem cronológica (mesmo critério da chave da agenda). */
    public static final Comparator<Atividade> ATIVIDADE_POR_DATA_HORA =
            Comparator.comparing(Atividade::getDataHoraInicio)
                      .thenComparing(Atividade::getTitulo);

    /**
     * Feed da tela inicial: o mais recente no topo.
     * Tipado em {@link ItemFeed}, então o <b>mesmo</b> comparador ordena
     * {@code Mensagem} e {@code Notificacao} na mesma lista — é o polimorfismo
     * de herança sendo aproveitado pela estrutura de dados.
     */
    public static final Comparator<ItemFeed> FEED_MAIS_RECENTE_PRIMEIRO =
            Comparator.comparing(ItemFeed::getCriadoEm).reversed()
                      .thenComparing(ItemFeed::getTitulo);
}

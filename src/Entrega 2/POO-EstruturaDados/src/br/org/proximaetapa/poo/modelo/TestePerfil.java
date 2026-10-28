package br.org.proximaetapa.poo.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Questionário de orientação educacional/profissional (RFM10). Agrupa as
 * {@link Pergunta}s das tabelas {@code perguntas_perfil} /
 * {@code alternativas_perfil} e sabe apurar o {@link ResultadoPerfil}.
 *
 * <p>ESTRUTURA DE DADOS: as perguntas ficam em um
 * {@code TreeMap<Integer, Pergunta>} com a <b>ordem</b> como chave. Motivo de
 * domínio: o teste tem que ser exibido sempre na mesma sequência pedagógica, e
 * o endpoint do backend faz {@code ORDER BY ordem}. O TreeMap mantém essa
 * ordenação automaticamente a cada inserção (O(log n)), então
 * {@link #getPerguntasEmOrdem()} nunca precisa ordenar nada — basta iterar.</p>
 *
 * <p>A apuração usa um {@code EnumMap<Perfil, Integer>}: como as chaves são um
 * {@code enum} fechado, o EnumMap é implementado como um vetor indexado pelo
 * ordinal — mais rápido e mais enxuto que um HashMap para esse caso.</p>
 */
public class TestePerfil implements Identificavel {

    private final int id;
    private String titulo;
    private String descricao;
    private final TreeMap<Integer, Pergunta> perguntasPorOrdem = new TreeMap<>();

    public TestePerfil(int id, String titulo, String descricao) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título do teste é obrigatório.");
        }
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
    }

    @Override
    public int getId() { return id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    /** REGRA: duas perguntas não podem ocupar a mesma posição no questionário. */
    public TestePerfil adicionarPergunta(Pergunta pergunta) {
        if (perguntasPorOrdem.containsKey(pergunta.getOrdem())) {
            throw new IllegalArgumentException(
                    "Já existe uma pergunta na ordem " + pergunta.getOrdem());
        }
        perguntasPorOrdem.put(pergunta.getOrdem(), pergunta);
        return this;
    }

    /** Perguntas na sequência correta — o TreeMap já as devolve ordenadas. */
    public List<Pergunta> getPerguntasEmOrdem() {
        return Collections.unmodifiableList(new ArrayList<>(perguntasPorOrdem.values()));
    }

    public Optional<Pergunta> localizarPorOrdem(int ordem) {
        return Optional.ofNullable(perguntasPorOrdem.get(ordem));
    }

    public Optional<Pergunta> localizarPorId(int perguntaId) {
        for (Pergunta p : perguntasPorOrdem.values()) {
            if (p.getId() == perguntaId) return Optional.of(p);
        }
        return Optional.empty();
    }

    public int totalDePerguntas() { return perguntasPorOrdem.size(); }

    /**
     * Apura o resultado do teste a partir das respostas enviadas pelo app,
     * no mesmo formato do backend: {@code {pergunta_id -> alternativa_id}}.
     *
     * <p>Conta quantas alternativas de cada perfil foram escolhidas e elege o
     * perfil mais votado. Empates são resolvidos pela ordem de declaração do
     * {@code enum}, o que torna o resultado determinístico.</p>
     *
     * @throws IllegalStateException se nenhuma resposta válida for reconhecida.
     */
    public ResultadoPerfil apurar(int resultadoId, int alunoId, Map<Integer, Integer> respostas) {
        if (respostas == null || respostas.isEmpty()) {
            throw new IllegalStateException("Envie as respostas do teste.");
        }

        Map<Alternativa.Perfil, Integer> contagem = new EnumMap<>(Alternativa.Perfil.class);
        int reconhecidas = 0;

        for (Map.Entry<Integer, Integer> resposta : respostas.entrySet()) {
            Optional<Pergunta> pergunta = localizarPorId(resposta.getKey());
            if (pergunta.isEmpty()) continue;
            Optional<Alternativa> alternativa = pergunta.get().localizarAlternativa(resposta.getValue());
            if (alternativa.isEmpty()) continue;
            Alternativa.Perfil perfil = alternativa.get().getPerfil();
            contagem.merge(perfil, 1, Integer::sum);
            reconhecidas++;
        }

        if (reconhecidas == 0) {
            throw new IllegalStateException("Nenhuma resposta válida foi reconhecida no teste.");
        }

        Alternativa.Perfil vencedor = Alternativa.Perfil.INDEFINIDO;
        int maior = 0;
        for (Map.Entry<Alternativa.Perfil, Integer> e : contagem.entrySet()) {
            if (e.getValue() > maior) {
                maior = e.getValue();
                vencedor = e.getKey();
            }
        }

        return new ResultadoPerfil(resultadoId, alunoId, respostas, vencedor, contagem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestePerfil)) return false;
        return id == ((TestePerfil) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("TestePerfil#%d [%s | %d perguntas]",
                id, titulo, perguntasPorOrdem.size());
    }
}

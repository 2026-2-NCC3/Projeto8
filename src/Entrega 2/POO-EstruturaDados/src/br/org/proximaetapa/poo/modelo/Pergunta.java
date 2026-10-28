package br.org.proximaetapa.poo.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pergunta do teste de perfil (tabela {@code perguntas_perfil}).
 *
 * <p>ESTRUTURA DE DADOS INTERNA: as alternativas ficam em um
 * {@code LinkedHashMap<Integer, Alternativa>} indexado pelo id da alternativa.
 * Motivo de domínio: o app envia as respostas como
 * {@code {pergunta_id: alternativa_id}}, então, ao corrigir o teste, o sistema
 * precisa achar a alternativa pelo id — com o LinkedHashMap isso é O(1), e a
 * ordem de exibição na tela (ordem de cadastro) é preservada, o que um
 * {@link HashMap} comum não garante.</p>
 */
public class Pergunta implements Identificavel {

    private final int id;
    private String texto;
    private int ordem;
    private final Map<Integer, Alternativa> alternativasPorId = new LinkedHashMap<>();

    public Pergunta(int id, String texto, int ordem) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Texto da pergunta é obrigatório.");
        }
        this.id = id;
        this.texto = texto;
        this.ordem = ordem;
    }

    @Override
    public int getId() { return id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    /** Posição da pergunta no questionário (chave do TreeMap de TestePerfil). */
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }

    /**
     * Acrescenta uma alternativa, validando que ela pertence a esta pergunta.
     * REGRA: não aceita duas alternativas com o mesmo id.
     */
    public Pergunta adicionarAlternativa(Alternativa alternativa) {
        if (alternativa.getPerguntaId() != this.id) {
            throw new IllegalArgumentException(
                    "Alternativa " + alternativa.getId() + " não pertence à pergunta " + id);
        }
        if (alternativasPorId.containsKey(alternativa.getId())) {
            throw new IllegalArgumentException(
                    "Alternativa " + alternativa.getId() + " já cadastrada nesta pergunta.");
        }
        alternativasPorId.put(alternativa.getId(), alternativa);
        return this; // permite encadear chamadas no cadastro
    }

    /** Busca a alternativa escolhida pelo aluno — O(1) pelo índice hash. */
    public Optional<Alternativa> localizarAlternativa(int alternativaId) {
        return Optional.ofNullable(alternativasPorId.get(alternativaId));
    }

    /** Alternativas na ordem em que devem aparecer na tela (cópia defensiva). */
    public List<Alternativa> getAlternativas() {
        return Collections.unmodifiableList(new ArrayList<>(alternativasPorId.values()));
    }

    public int totalDeAlternativas() { return alternativasPorId.size(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pergunta)) return false;
        return id == ((Pergunta) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Pergunta#%d [ordem %d | %s | %d alternativas]",
                id, ordem, texto, alternativasPorId.size());
    }
}

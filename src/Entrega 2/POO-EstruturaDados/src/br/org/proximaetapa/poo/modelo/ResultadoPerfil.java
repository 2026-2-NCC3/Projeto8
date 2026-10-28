package br.org.proximaetapa.poo.modelo;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Resultado apurado de um teste de perfil (tabela {@code resultados_perfil}).
 * O aluno pode refazer o teste, então existem vários resultados por aluno —
 * o app mostra o histórico em ordem decrescente de conclusão.
 *
 * <p>Objeto imutável: as respostas e a contagem recebidas no construtor são
 * copiadas e devolvidas como mapas não modificáveis. Um resultado já apurado
 * não pode ser alterado depois — é um registro histórico.</p>
 *
 * <p>As respostas ficam em um {@code TreeMap<Integer, Integer>} (pergunta ->
 * alternativa) para que a revisão da prova saia na ordem das perguntas,
 * reproduzindo o {@code respostas_json} do banco de forma estável.</p>
 */
public class ResultadoPerfil implements Identificavel {

    private final int id;
    private final int alunoId;
    private final Map<Integer, Integer> respostas;             // pergunta -> alternativa
    private final Alternativa.Perfil perfilResultado;
    private final Map<Alternativa.Perfil, Integer> contagem;   // perfil -> votos
    private final String descricao;
    private final LocalDateTime concluidoEm;

    public ResultadoPerfil(int id, int alunoId, Map<Integer, Integer> respostas,
                           Alternativa.Perfil perfilResultado,
                           Map<Alternativa.Perfil, Integer> contagem) {
        if (perfilResultado == null) {
            throw new IllegalArgumentException("O resultado precisa de um perfil apurado.");
        }
        this.id = id;
        this.alunoId = alunoId;
        this.respostas = Collections.unmodifiableMap(new TreeMap<>(respostas));
        this.perfilResultado = perfilResultado;
        Map<Alternativa.Perfil, Integer> copia = new EnumMap<>(Alternativa.Perfil.class);
        copia.putAll(contagem);
        this.contagem = Collections.unmodifiableMap(copia);
        this.descricao = perfilResultado.getDescricao();
        this.concluidoEm = LocalDateTime.now();
    }

    @Override
    public int getId() { return id; }

    public int getAlunoId() { return alunoId; }

    /** Respostas na ordem das perguntas (equivalente ao {@code respostas_json}). */
    public Map<Integer, Integer> getRespostas() { return respostas; }

    public Alternativa.Perfil getPerfilResultado() { return perfilResultado; }

    /** Quantas alternativas de cada perfil o aluno escolheu. */
    public Map<Alternativa.Perfil, Integer> getContagem() { return contagem; }

    public String getDescricao() { return descricao; }

    public LocalDateTime getConcluidoEm() { return concluidoEm; }

    /** Aviso ético obrigatório exibido junto ao resultado no app. */
    public String getAviso() {
        return "Este resultado tem finalidade de orientação educacional/profissional "
                + "e não é um diagnóstico psicológico.";
    }

    /** Contagem formatada para exibição, do perfil mais votado para o menos. */
    public Map<String, Integer> getContagemFormatada() {
        Map<String, Integer> saida = new LinkedHashMap<>();
        contagem.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> saida.put(e.getKey().getCodigo(), e.getValue()));
        return saida;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResultadoPerfil)) return false;
        return id == ((ResultadoPerfil) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("ResultadoPerfil#%d [aluno=%d | perfil=%s | %d respostas | %s]",
                id, alunoId, perfilResultado.getCodigo(), respostas.size(), getContagemFormatada());
    }
}

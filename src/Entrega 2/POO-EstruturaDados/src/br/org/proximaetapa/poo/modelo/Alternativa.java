package br.org.proximaetapa.poo.modelo;

import java.util.Objects;

/**
 * Alternativa de resposta de uma pergunta do teste de perfil
 * (tabela {@code alternativas_perfil}).
 *
 * <p>Cada alternativa pontua para exatamente um {@link Perfil}. No banco esse
 * campo é um TEXT livre ('exatas', 'humanas', ...); em Java ele virou
 * {@code enum}, o que impede valor inválido em tempo de compilação e já carrega
 * a descrição exibida no resultado do teste.</p>
 */
public class Alternativa implements Identificavel {

    /** Áreas de interesse apuradas pelo teste de perfil (RFM10). */
    public enum Perfil {
        EXATAS("exatas", "Você tem afinidade com raciocínio lógico, matemática e tecnologia."),
        HUMANAS("humanas", "Você se destaca em comunicação, argumentação e temas sociais."),
        SAUDE("saude", "Você demonstra cuidado com o próximo e interesse por biologia e saúde."),
        ARTES("artes", "Você tem perfil criativo e expressivo."),
        INDEFINIDO("indefinido", "Responda mais perguntas para apurarmos seu perfil.");

        private final String codigo;
        private final String descricao;

        Perfil(String codigo, String descricao) {
            this.codigo = codigo;
            this.descricao = descricao;
        }

        /** Código gravado na coluna {@code perfil} do banco. */
        public String getCodigo() { return codigo; }

        public String getDescricao() { return descricao; }
    }

    private final int id;
    private final int perguntaId;
    private final String texto;
    private final Perfil perfil;

    public Alternativa(int id, int perguntaId, String texto, Perfil perfil) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Texto da alternativa é obrigatório.");
        }
        if (perfil == null) {
            throw new IllegalArgumentException("Toda alternativa pontua para um perfil.");
        }
        this.id = id;
        this.perguntaId = perguntaId;
        this.texto = texto;
        this.perfil = perfil;
    }

    @Override
    public int getId() { return id; }

    public int getPerguntaId() { return perguntaId; }

    public String getTexto() { return texto; }

    public Perfil getPerfil() { return perfil; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alternativa)) return false;
        return id == ((Alternativa) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Alternativa#%d [pergunta=%d | %s | pontua %s]",
                id, perguntaId, texto, perfil.getCodigo());
    }
}

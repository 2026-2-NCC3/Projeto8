package br.org.proximaetapa.poo.modelo;

/**
 * Contrato comum a toda entidade de domínio que possui identidade própria
 * (um id vindo da chave primária da tabela correspondente no banco).
 *
 * <p><b>Por que existe (ABSTRAÇÃO + POLIMORFISMO):</b> na Entrega 1 o
 * {@code RepositorioEmMemoria<T>} precisava receber no construtor uma função
 * {@code Function<T, Integer>} só para saber como extrair o id de cada objeto.
 * Ao exigir {@code T extends Identificavel}, o repositório passa a chamar
 * {@code item.getId()} diretamente: o mesmo código genérico atende Aluno,
 * Curso, Atividade, Inscrição, Presença, Certificado etc., porque todas
 * respondem ao mesmo contrato. É polimorfismo de interface aplicado à
 * estrutura de dados.</p>
 */
public interface Identificavel {

    /** Identificador único da entidade (chave primária). */
    int getId();
}

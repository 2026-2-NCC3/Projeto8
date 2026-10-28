package br.org.proximaetapa.poo.servico;

/**
 * Retorno padrão das operações de serviço: diz se a regra de negócio foi
 * aceita e traz a mensagem que o app exibe ao aluno.
 *
 * <p>Motivo do desenho: as regras do projeto (curso lotado, presença
 * duplicada, frequência insuficiente) são <b>recusas previstas</b>, não erros de
 * programação. Devolver um {@code Resultado} em vez de lançar exceção deixa
 * claro no código que são caminhos esperados, e o texto já sai pronto para a
 * tela — exatamente como o backend responde 409/403 com um JSON de erro.</p>
 *
 * @param ok       se a operação foi concluída
 * @param mensagem texto exibido ao usuário
 * @param dado     objeto produzido pela operação (inscrição, presença,
 *                 certificado...), ou {@code null} em caso de recusa
 */
public record Resultado(boolean ok, String mensagem, Object dado) {

    public static Resultado sucesso(String mensagem, Object dado) {
        return new Resultado(true, mensagem, dado);
    }

    public static Resultado recusa(String mensagem) {
        return new Resultado(false, mensagem, null);
    }

    /** Linha formatada para o console da demonstração. */
    public String linha() {
        return (ok ? "OK   " : "RECUSA ") + mensagem;
    }

    @Override
    public String toString() { return linha(); }
}

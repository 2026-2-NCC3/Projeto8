package br.org.proximaetapa.poo.repositorio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

/**
 * Fila de check-ins feitos <b>sem internet</b>, aguardando envio ao servidor
 * (RFM15 — persistência local com sincronização após reconexão).
 *
 * <h2>Por que Queue / LinkedList (FIFO)</h2>
 * As oficinas da ONG acontecem em escolas e centros comunitários onde o sinal
 * costuma falhar. O app grava o check-in localmente e, quando a conexão volta,
 * envia os pendentes. A ordem importa de verdade:
 * <ul>
 *   <li>a presença tem que chegar ao servidor na <b>mesma ordem em que o aluno
 *       passou o QR Code</b>, porque o {@code registrado_em} é o comprovante do
 *       horário de entrada;</li>
 *   <li>se os itens fossem reenviados fora de ordem, dois check-ins do mesmo
 *       dia apareceriam invertidos no histórico do aluno.</li>
 * </ul>
 * {@code Queue} é a estrutura que expressa exatamente essa semântica
 * primeiro-a-entrar-primeiro-a-sair. A implementação escolhida é
 * {@link LinkedList} porque {@code offer} (entrar no fim) e {@code poll} (sair
 * do começo) são <b>O(1)</b>, sem o deslocamento de elementos que um
 * {@code ArrayList.remove(0)} causaria (O(n) a cada envio).
 *
 * <p>Usar a interface {@code Queue} na declaração e {@code LinkedList} só na
 * criação é ABSTRAÇÃO: trocar por {@code ArrayDeque} amanhã não muda uma linha
 * do resto da classe.</p>
 */
public class FilaDeSincronizacao {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Check-in guardado no celular: o QR Code lido, quem leu e quando.
     * O id da atividade não é gravado porque o app offline não consulta o
     * servidor — ele só tem a string do QR Code em mãos.
     */
    public record CheckInPendente(int alunoId, String qrCode, LocalDateTime lidoEm) {
        @Override
        public String toString() {
            return String.format("check-in aluno=%d qr=%s lido as %s",
                    alunoId, qrCode, lidoEm.format(HORA));
        }
    }

    /** Resultado do processamento de um item pela rotina de envio. */
    public record ResultadoEnvio(CheckInPendente pendente, boolean aceito, String motivo) { }

    private final Queue<CheckInPendente> pendentes = new LinkedList<>();

    /** ENFILEIRAR no fim da fila — O(1). */
    public void enfileirar(CheckInPendente checkIn) {
        if (checkIn == null) {
            throw new IllegalArgumentException("Check-in pendente não pode ser nulo.");
        }
        pendentes.offer(checkIn);
    }

    /** Espia o próximo a ser enviado sem retirá-lo da fila — O(1). */
    public CheckInPendente espiarProximo() {
        return pendentes.peek();
    }

    /**
     * DRENAR a fila quando a conexão volta: retira um a um, do mais antigo para
     * o mais novo, e entrega cada item à rotina de envio recebida como
     * parâmetro.
     *
     * <p>O parâmetro {@code Function} desacopla a estrutura de dados da regra de
     * negócio: a fila não sabe o que é "registrar presença", só garante a ordem.
     * Quem sabe disso é o {@code ServicoPresenca}.</p>
     *
     * @return o relatório de cada envio, na ordem em que foram processados.
     */
    public List<ResultadoEnvio> drenar(Function<CheckInPendente, ResultadoEnvio> rotinaDeEnvio) {
        List<ResultadoEnvio> relatorio = new ArrayList<>();
        CheckInPendente atual;
        while ((atual = pendentes.poll()) != null) {   // poll: retira do início, O(1)
            relatorio.add(rotinaDeEnvio.apply(atual));
        }
        return relatorio;
    }

    /** Conteúdo atual da fila, na ordem de envio (apenas para exibição). */
    public List<CheckInPendente> listarPendentes() {
        return Collections.unmodifiableList(new ArrayList<>(pendentes));
    }

    public int tamanho() {
        return pendentes.size();
    }

    public boolean estaVazia() {
        return pendentes.isEmpty();
    }
}

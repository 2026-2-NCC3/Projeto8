package br.org.proximaetapa.app.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Um check-in feito pelo aluno SEM internet, esperando para ser enviado
 * (RFM06 + RFM15). É o coração da sincronização da Entrega 2.
 *
 * O problema que ela resolve: os encontros acontecem em escolas e espaços
 * comunitários onde o sinal costuma falhar. Sem esta fila, o aluno escaneia o
 * QR Code, o envio falha e a presença dele simplesmente se perde — e
 * frequência é justamente o que decide se ele recebe o certificado.
 *
 * Como funciona: o check-in é gravado aqui na hora, com o horário real do
 * celular, e o aluno já vê "presença registrada". Quando a conexão volta, a
 * fila é enviada em ordem de chegada (FIFO) e cada item só sai daqui depois
 * que o servidor confirma.
 *
 * A coluna `uuidLocal` é gerada no celular e viaja junto com o check-in. É ela
 * que torna o reenvio seguro: se a resposta do servidor se perder no caminho e
 * o app tentar de novo, o servidor reconhece o mesmo identificador e não
 * registra a presença duas vezes (idempotência).
 */
@Entity(
        tableName = "presencas_pendentes",
        indices = { @Index(value = "uuidLocal", unique = true) }
)
public class PresencaPendente {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Identificador único gerado no celular (UUID). */
    public String uuidLocal;

    /** Conteúdo lido do QR Code do encontro. */
    public String qrCode;

    /** Momento real do check-in, no celular — não o momento do envio. */
    public String registradoEm;

    /** Nome do encontro, só para mostrar na tela enquanto está pendente. */
    public String descricao;

    /** Quantas vezes já tentamos enviar — usado para não insistir para sempre. */
    public int tentativas;

    /** Última mensagem de erro devolvida pelo servidor, se houve. */
    public String ultimoErro;

    public PresencaPendente() {
    }

    public PresencaPendente(String uuidLocal, String qrCode, String registradoEm, String descricao) {
        this.uuidLocal = uuidLocal;
        this.qrCode = qrCode;
        this.registradoEm = registradoEm;
        this.descricao = descricao;
        this.tentativas = 0;
    }
}

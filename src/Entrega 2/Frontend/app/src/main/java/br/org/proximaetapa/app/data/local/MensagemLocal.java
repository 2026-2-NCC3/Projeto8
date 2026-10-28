package br.org.proximaetapa.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.proximaetapa.app.model.Mensagem;

/** Comunicado da organização guardado localmente (RFM11). */
@Entity(tableName = "mensagens")
public class MensagemLocal {

    @PrimaryKey
    public int id;

    public String titulo;
    public String corpo;
    public String destinatarioTipo;
    public String criadoEm;

    public MensagemLocal() {
    }

    @NonNull
    public static MensagemLocal de(Mensagem mensagem) {
        MensagemLocal local = new MensagemLocal();
        local.id = mensagem.id;
        local.titulo = mensagem.titulo;
        local.corpo = mensagem.corpo;
        local.destinatarioTipo = mensagem.destinatario_tipo;
        local.criadoEm = mensagem.criado_em;
        return local;
    }
}

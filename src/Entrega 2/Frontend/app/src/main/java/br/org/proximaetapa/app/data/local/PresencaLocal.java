package br.org.proximaetapa.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.proximaetapa.app.model.Presenca;

/** Presença já confirmada pelo servidor, guardada para o histórico offline (RFM07). */
@Entity(tableName = "presencas")
public class PresencaLocal {

    @PrimaryKey
    public int id;

    public int atividadeId;
    public String registradoEm;
    public String atividade;
    public String data;
    public String curso;

    public PresencaLocal() {
    }

    @NonNull
    public static PresencaLocal de(Presenca presenca) {
        PresencaLocal local = new PresencaLocal();
        local.id = presenca.id;
        local.atividadeId = presenca.atividade_id;
        local.registradoEm = presenca.registrado_em;
        local.atividade = presenca.atividade;
        local.data = presenca.data;
        local.curso = presenca.curso;
        return local;
    }
}

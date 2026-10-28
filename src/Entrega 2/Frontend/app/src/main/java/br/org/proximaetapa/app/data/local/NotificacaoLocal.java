package br.org.proximaetapa.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.proximaetapa.app.model.Notificacao;

/** Notificação guardada localmente (RFM13). */
@Entity(tableName = "notificacoes")
public class NotificacaoLocal {

    @PrimaryKey
    public int id;

    public String tipo;
    public String titulo;
    public String corpo;
    public boolean lida;
    public String criadoEm;

    /**
     * Marca se o app já avisou o aluno sobre esta notificação na barra de
     * status. Sem isso, toda sincronização reexibiria as mesmas notificações.
     */
    public boolean exibida;

    public NotificacaoLocal() {
    }

    @NonNull
    public static NotificacaoLocal de(Notificacao notificacao) {
        NotificacaoLocal local = new NotificacaoLocal();
        local.id = notificacao.id;
        local.tipo = notificacao.tipo;
        local.titulo = notificacao.titulo;
        local.corpo = notificacao.corpo;
        local.lida = notificacao.lida == 1;
        local.criadoEm = notificacao.criado_em;
        local.exibida = false;
        return local;
    }
}

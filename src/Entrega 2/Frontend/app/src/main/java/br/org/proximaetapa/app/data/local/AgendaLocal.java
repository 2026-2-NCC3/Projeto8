package br.org.proximaetapa.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.proximaetapa.app.model.AgendaItem;

/** Cópia local de um encontro da agenda do aluno (RFM05 + RFM15). */
@Entity(tableName = "agenda")
public class AgendaLocal {

    @PrimaryKey
    public int id;

    public String titulo;
    public String data;
    public String horaInicio;
    public String horaFim;
    public String local;
    public String curso;
    public int cursoId;
    public boolean presencaConfirmada;

    public AgendaLocal() {
    }

    @NonNull
    public static AgendaLocal de(AgendaItem item) {
        AgendaLocal local = new AgendaLocal();
        local.id = item.id;
        local.titulo = item.titulo;
        local.data = item.data;
        local.horaInicio = item.hora_inicio;
        local.horaFim = item.hora_fim;
        local.local = item.local;
        local.curso = item.curso;
        local.cursoId = item.curso_id;
        local.presencaConfirmada = item.presenca_confirmada;
        return local;
    }

    @NonNull
    public AgendaItem paraModelo() {
        AgendaItem item = new AgendaItem();
        item.id = id;
        item.titulo = titulo;
        item.data = data;
        item.hora_inicio = horaInicio;
        item.hora_fim = horaFim;
        item.local = local;
        item.curso = curso;
        item.curso_id = cursoId;
        item.presenca_confirmada = presencaConfirmada;
        return item;
    }
}

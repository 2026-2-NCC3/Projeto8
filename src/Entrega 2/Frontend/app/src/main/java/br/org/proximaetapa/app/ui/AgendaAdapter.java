package br.org.proximaetapa.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.model.AgendaItem;
import br.org.proximaetapa.app.util.Formato;

/** Adapter do RecyclerView da agenda individual (RFM05). */
public class AgendaAdapter extends RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder> {

    private final List<AgendaItem> eventos = new ArrayList<>();

    public void atualizar(List<AgendaItem> novaLista) {
        eventos.clear();
        eventos.addAll(novaLista);
        notifyDataSetChanged();
    }

    public boolean estaVazio() {
        return eventos.isEmpty();
    }

    @NonNull
    @Override
    public AgendaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_agenda, parent, false);
        return new AgendaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AgendaViewHolder holder, int position) {
        AgendaItem evento = eventos.get(position);

        holder.dataHora.setText(holder.itemView.getContext().getString(
                R.string.data_as_hora, Formato.diaMes(evento.data), Formato.hora(evento.hora_inicio)));
        holder.titulo.setText(evento.titulo);
        holder.cursoLocal.setText(evento.curso + (evento.local != null ? " · " + evento.local : ""));

        // Verde quando a presença já consta; cinza quando ainda não — dizer
        // "pendente" em vermelho assustaria quem simplesmente ainda não foi ao
        // encontro, que é o caso normal de qualquer evento futuro.
        boolean confirmada = evento.presenca_confirmada;
        holder.presenca.setText(confirmada
                ? R.string.presenca_confirmada
                : R.string.presenca_pendente);
        holder.presenca.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                confirmada ? R.color.primary : R.color.text_secondary));
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    static class AgendaViewHolder extends RecyclerView.ViewHolder {
        final TextView dataHora;
        final TextView titulo;
        final TextView cursoLocal;
        final TextView presenca;

        AgendaViewHolder(@NonNull View itemView) {
            super(itemView);
            dataHora = itemView.findViewById(R.id.txtDataHora);
            titulo = itemView.findViewById(R.id.txtTituloAtividade);
            cursoLocal = itemView.findViewById(R.id.txtCursoLocal);
            presenca = itemView.findViewById(R.id.txtPresencaStatus);
        }
    }
}

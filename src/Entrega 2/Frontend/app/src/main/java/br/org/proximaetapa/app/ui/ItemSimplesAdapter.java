package br.org.proximaetapa.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.org.proximaetapa.app.R;

/**
 * Adaptador de lista reaproveitado por várias telas (histórico de presença,
 * certificados, comunicados e notificações).
 *
 * Essas quatro listas têm a mesma forma — título, descrição, data e um selo de
 * status — e só mudam o conteúdo. Um adaptador só, com um layout só, evita
 * quatro cópias quase idênticas e faz todas as listas do app parecerem parte
 * do mesmo produto.
 */
public class ItemSimplesAdapter extends RecyclerView.Adapter<ItemSimplesAdapter.ItemViewHolder> {

    /** Uma linha da lista, já pronta para exibição. */
    public static class Item {
        public final String titulo;
        public final String subtitulo;
        public final String detalhe;
        public final String selo;
        public final boolean destacado;
        /** Objeto original, para a tela saber o que foi tocado. */
        public final Object referencia;

        public Item(String titulo, String subtitulo, String detalhe, String selo,
                    boolean destacado, Object referencia) {
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.detalhe = detalhe;
            this.selo = selo;
            this.destacado = destacado;
            this.referencia = referencia;
        }
    }

    public interface AoTocar {
        void tocou(Item item);
    }

    private final List<Item> itens = new ArrayList<>();
    private final AoTocar listener;

    public ItemSimplesAdapter(AoTocar listener) {
        this.listener = listener;
    }

    public void atualizar(List<Item> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_simples, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itens.get(position);

        holder.titulo.setText(item.titulo);
        definirTexto(holder.subtitulo, item.subtitulo);
        definirTexto(holder.detalhe, item.detalhe);
        definirTexto(holder.selo, item.selo);

        // O destaque marca o que exige atenção: notificação não lida,
        // presença ainda não enviada, frequência abaixo do mínimo.
        holder.marcador.setVisibility(item.destacado ? View.VISIBLE : View.INVISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.tocou(item);
        });
    }

    /** Esconde o campo quando não há texto, em vez de deixar um espaço vazio. */
    private void definirTexto(TextView campo, String texto) {
        if (texto == null || texto.isEmpty()) {
            campo.setVisibility(View.GONE);
        } else {
            campo.setVisibility(View.VISIBLE);
            campo.setText(texto);
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView titulo;
        final TextView subtitulo;
        final TextView detalhe;
        final TextView selo;
        final View marcador;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.txtItemTitulo);
            subtitulo = itemView.findViewById(R.id.txtItemSubtitulo);
            detalhe = itemView.findViewById(R.id.txtItemDetalhe);
            selo = itemView.findViewById(R.id.txtItemSelo);
            marcador = itemView.findViewById(R.id.marcadorItem);
        }
    }
}

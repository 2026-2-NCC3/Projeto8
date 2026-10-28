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
import br.org.proximaetapa.app.model.ChatMensagem;
import br.org.proximaetapa.app.util.Formato;

/**
 * Conversa do chat de dúvidas (RFM12).
 *
 * Usa dois tipos de linha — a fala do aluno e a fala da equipe — para que cada
 * uma apareça de um lado da tela, do jeito que qualquer aplicativo de mensagem
 * mostra. É o caso clássico em que `getItemViewType` vale a pena: o conteúdo é
 * o mesmo, mas a apresentação muda.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_ALUNO = 1;
    private static final int TIPO_EQUIPE = 2;

    private final List<ChatMensagem> mensagens = new ArrayList<>();

    public void atualizar(List<ChatMensagem> novas) {
        mensagens.clear();
        mensagens.addAll(novas);
        notifyDataSetChanged();
    }

    /** Acrescenta uma fala sem recarregar a lista inteira (envio otimista). */
    public void acrescentar(ChatMensagem mensagem) {
        mensagens.add(mensagem);
        notifyItemInserted(mensagens.size() - 1);
    }

    public int tamanho() {
        return mensagens.size();
    }

    @Override
    public int getItemViewType(int position) {
        return "aluno".equals(mensagens.get(position).autor) ? TIPO_ALUNO : TIPO_EQUIPE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TIPO_ALUNO
                ? R.layout.item_chat_aluno
                : R.layout.item_chat_equipe;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new FalaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMensagem mensagem = mensagens.get(position);
        FalaViewHolder fala = (FalaViewHolder) holder;
        fala.texto.setText(mensagem.texto);
        fala.hora.setText(Formato.dataHora(mensagem.criado_em));
    }

    @Override
    public int getItemCount() {
        return mensagens.size();
    }

    static class FalaViewHolder extends RecyclerView.ViewHolder {
        final TextView texto;
        final TextView hora;

        FalaViewHolder(@NonNull View itemView) {
            super(itemView);
            texto = itemView.findViewById(R.id.txtTextoFala);
            hora = itemView.findViewById(R.id.txtHoraFala);
        }
    }
}

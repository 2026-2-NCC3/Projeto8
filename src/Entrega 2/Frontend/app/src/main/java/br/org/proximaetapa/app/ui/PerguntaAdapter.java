package br.org.proximaetapa.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.model.Alternativa;
import br.org.proximaetapa.app.model.Pergunta;

/**
 * Questionário do teste de perfil (RFM10).
 *
 * Cada pergunta vira um cartão com suas alternativas em um RadioGroup. As
 * respostas ficam guardadas aqui, em um mapa pergunta → alternativa, porque a
 * RecyclerView recicla as views: se dependêssemos do estado dos botões na
 * tela, a resposta de uma pergunta reapareceria em outra ao rolar a lista.
 */
public class PerguntaAdapter extends RecyclerView.Adapter<PerguntaAdapter.PerguntaViewHolder> {

    private final List<Pergunta> perguntas = new ArrayList<>();
    private final Map<Integer, Integer> respostas = new HashMap<>();

    public void atualizar(List<Pergunta> novas) {
        perguntas.clear();
        perguntas.addAll(novas);
        respostas.clear();
        notifyDataSetChanged();
    }

    public Map<Integer, Integer> getRespostas() {
        return respostas;
    }

    public boolean respondeuTudo() {
        return respostas.size() == perguntas.size() && !perguntas.isEmpty();
    }

    public int quantasFaltam() {
        return perguntas.size() - respostas.size();
    }

    @NonNull
    @Override
    public PerguntaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pergunta, parent, false);
        return new PerguntaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PerguntaViewHolder holder, int position) {
        Pergunta pergunta = perguntas.get(position);

        holder.numero.setText(holder.itemView.getContext()
                .getString(R.string.pergunta_numero, position + 1, perguntas.size()));
        holder.texto.setText(pergunta.texto);

        // Importante limpar o listener ANTES de remontar as alternativas:
        // sem isso, recriar os botões dispara o callback e grava uma resposta
        // que o aluno não escolheu.
        holder.grupo.setOnCheckedChangeListener(null);
        holder.grupo.removeAllViews();

        if (pergunta.alternativas != null) {
            for (Alternativa alternativa : pergunta.alternativas) {
                RadioButton botao = new RadioButton(holder.itemView.getContext());
                botao.setText(alternativa.texto);
                // O id da alternativa vira o id da view, para recuperá-lo
                // direto no callback sem buscas adicionais.
                botao.setId(alternativa.id);
                holder.grupo.addView(botao);
            }
        }

        Integer jaRespondida = respostas.get(pergunta.id);
        if (jaRespondida != null) {
            holder.grupo.check(jaRespondida);
        } else {
            holder.grupo.clearCheck();
        }

        holder.grupo.setOnCheckedChangeListener((grupo, idSelecionado) -> {
            if (idSelecionado != -1) {
                respostas.put(pergunta.id, idSelecionado);
            }
        });
    }

    @Override
    public int getItemCount() {
        return perguntas.size();
    }

    static class PerguntaViewHolder extends RecyclerView.ViewHolder {
        final TextView numero;
        final TextView texto;
        final RadioGroup grupo;

        PerguntaViewHolder(@NonNull View itemView) {
            super(itemView);
            numero = itemView.findViewById(R.id.txtNumeroPergunta);
            texto = itemView.findViewById(R.id.txtTextoPergunta);
            grupo = itemView.findViewById(R.id.grupoAlternativas);
        }
    }
}

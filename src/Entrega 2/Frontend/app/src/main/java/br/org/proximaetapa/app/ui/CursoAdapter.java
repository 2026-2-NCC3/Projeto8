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
import br.org.proximaetapa.app.model.Curso;

/** Adapter do RecyclerView de cursos (RFM03). */
public class CursoAdapter extends RecyclerView.Adapter<CursoAdapter.CursoViewHolder> {

    public interface OnCursoClicado {
        void aoClicar(Curso curso);
    }

    private final List<Curso> cursos = new ArrayList<>();
    private final OnCursoClicado listener;

    public CursoAdapter(OnCursoClicado listener) {
        this.listener = listener;
    }

    public void atualizar(List<Curso> novaLista) {
        cursos.clear();
        cursos.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CursoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_curso, parent, false);
        return new CursoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CursoViewHolder holder, int position) {
        Curso curso = cursos.get(position);
        holder.titulo.setText(curso.titulo);
        holder.local.setText(curso.local != null ? curso.local : "Local a definir");
        holder.cargaHoraria.setText(curso.carga_horaria + "h"); // dado numérico exibido na lista
        holder.vagas.setText(curso.vagas_disponiveis + " vaga(s) disponível(is)");
        holder.itemView.setOnClickListener(v -> listener.aoClicar(curso));
    }

    @Override
    public int getItemCount() {
        return cursos.size();
    }

    static class CursoViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, local, cargaHoraria, vagas;

        CursoViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.txtTituloCurso);
            local = itemView.findViewById(R.id.txtLocalCurso);
            cargaHoraria = itemView.findViewById(R.id.txtCargaHoraria);
            vagas = itemView.findViewById(R.id.txtVagas);
        }
    }
}

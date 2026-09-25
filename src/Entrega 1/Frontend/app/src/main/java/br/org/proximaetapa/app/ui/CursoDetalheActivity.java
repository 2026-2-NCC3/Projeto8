package br.org.proximaetapa.app.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.model.Curso;

/** Tela de detalhe de um curso, aberta a partir da lista (Intent com extra). */
public class CursoDetalheActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO = "extra_curso";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curso_detalhe);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        Curso curso = (Curso) getIntent().getSerializableExtra(EXTRA_CURSO);

        TextView txtTitulo = findViewById(R.id.txtTitulo);
        TextView txtDescricao = findViewById(R.id.txtDescricao);
        TextView txtDetalhes = findViewById(R.id.txtDetalhes);

        if (curso != null) {
            txtTitulo.setText(curso.titulo);
            txtDescricao.setText(curso.descricao);
            txtDetalhes.setText(String.format(
                "Local: %s\nCarga horária: %dh\nVagas: %d de %d disponíveis\nPeríodo: %s a %s",
                curso.local, curso.carga_horaria, curso.vagas_disponiveis, curso.vagas_totais,
                curso.data_inicio, curso.data_fim));
        }
    }
}

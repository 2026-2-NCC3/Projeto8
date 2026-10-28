package br.org.proximaetapa.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.proximaetapa.app.model.Aluno;

/** Dados do aluno logado, para o perfil e o card abrirem offline (RFM08/RFM14). */
@Entity(tableName = "aluno")
public class AlunoLocal {

    @PrimaryKey
    public int id;

    public String nome;
    public String email;
    public String escola;
    public String serie;
    public String cidade;
    public int pontos;
    public String nivel;

    public AlunoLocal() {
    }

    @NonNull
    public static AlunoLocal de(Aluno aluno) {
        AlunoLocal local = new AlunoLocal();
        local.id = aluno.id;
        local.nome = aluno.nome;
        local.email = aluno.email;
        local.escola = aluno.escola;
        local.serie = aluno.serie;
        local.cidade = aluno.cidade;
        local.pontos = aluno.pontos;
        local.nivel = aluno.nivel;
        return local;
    }

    @NonNull
    public Aluno paraModelo() {
        Aluno aluno = new Aluno();
        aluno.id = id;
        aluno.nome = nome;
        aluno.email = email;
        aluno.escola = escola;
        aluno.serie = serie;
        aluno.cidade = cidade;
        aluno.pontos = pontos;
        aluno.nivel = nivel;
        return aluno;
    }
}

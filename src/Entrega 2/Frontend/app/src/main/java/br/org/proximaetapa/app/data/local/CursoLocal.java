package br.org.proximaetapa.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.proximaetapa.app.model.Curso;

/**
 * Cópia local de um curso (RFM15 - persistência).
 *
 * O app guarda os cursos no banco do próprio celular para que a lista abra
 * instantaneamente e continue visível quando o aluno estiver sem internet —
 * situação comum no público atendido pela ONG.
 */
@Entity(tableName = "cursos")
public class CursoLocal {

    @PrimaryKey
    public int id;

    public String titulo;
    public String descricao;
    public String categoria;
    public String local;
    public int cargaHoraria;
    public String dataInicio;
    public String dataFim;
    public int vagasTotais;
    public int vagasDisponiveis;
    public int possuiCertificado;

    /** Status da inscrição do aluno neste curso, ou null se não estiver inscrito. */
    public String statusInscricao;

    public CursoLocal() {
    }

    /** Converte o objeto vindo da API no registro que será gravado no banco local. */
    @NonNull
    public static CursoLocal de(Curso curso) {
        CursoLocal local = new CursoLocal();
        local.id = curso.id;
        local.titulo = curso.titulo;
        local.descricao = curso.descricao;
        local.categoria = curso.categoria;
        local.local = curso.local;
        local.cargaHoraria = curso.carga_horaria;
        local.dataInicio = curso.data_inicio;
        local.dataFim = curso.data_fim;
        local.vagasTotais = curso.vagas_totais;
        local.vagasDisponiveis = curso.vagas_disponiveis;
        local.possuiCertificado = curso.possui_certificado;
        local.statusInscricao = curso.status_inscricao;
        return local;
    }

    /** Converte de volta para o modelo usado pelas telas. */
    @NonNull
    public Curso paraModelo() {
        Curso curso = new Curso();
        curso.id = id;
        curso.titulo = titulo;
        curso.descricao = descricao;
        curso.categoria = categoria;
        curso.local = local;
        curso.carga_horaria = cargaHoraria;
        curso.data_inicio = dataInicio;
        curso.data_fim = dataFim;
        curso.vagas_totais = vagasTotais;
        curso.vagas_disponiveis = vagasDisponiveis;
        curso.possui_certificado = possuiCertificado;
        curso.status_inscricao = statusInscricao;
        return curso;
    }
}

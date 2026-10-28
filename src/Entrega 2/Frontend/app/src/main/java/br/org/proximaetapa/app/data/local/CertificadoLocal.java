package br.org.proximaetapa.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.proximaetapa.app.model.Certificado;

/** Certificado do aluno guardado localmente (RFM09). */
@Entity(tableName = "certificados")
public class CertificadoLocal {

    @PrimaryKey
    public int id;

    public int cursoId;
    public String certificadoUrl;
    public String emitidoEm;
    public String curso;
    public int cargaHoraria;

    public CertificadoLocal() {
    }

    @NonNull
    public static CertificadoLocal de(Certificado certificado) {
        CertificadoLocal local = new CertificadoLocal();
        local.id = certificado.id;
        local.cursoId = certificado.curso_id;
        local.certificadoUrl = certificado.certificado_url;
        local.emitidoEm = certificado.emitido_em;
        local.curso = certificado.curso;
        local.cargaHoraria = certificado.carga_horaria;
        return local;
    }
}

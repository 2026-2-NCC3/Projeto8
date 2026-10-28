package br.org.proximaetapa.app.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Banco local do app (SQLite, via Room) — exigência da Entrega 2.
 *
 * Instância única (singleton) porque abrir o banco é caro e o Room já cuida de
 * sincronizar o acesso entre as threads.
 */
@Database(
        entities = {
                AlunoLocal.class,
                CursoLocal.class,
                AgendaLocal.class,
                PresencaLocal.class,
                PresencaPendente.class,
                CertificadoLocal.class,
                NotificacaoLocal.class,
                MensagemLocal.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract DadosDao dao();

    private static volatile AppDatabase instancia;

    public static AppDatabase obter(Context contexto) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    contexto.getApplicationContext(),
                                    AppDatabase.class,
                                    "proxima_etapa.db")
                            // Em um protótipo acadêmico, recriar o banco quando o
                            // schema muda é aceitável: os dados do aluno vivem no
                            // servidor e voltam na próxima sincronização.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instancia;
    }
}

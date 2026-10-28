package br.org.proximaetapa.app.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import br.org.proximaetapa.app.R;
import br.org.proximaetapa.app.ui.NotificacoesActivity;

/**
 * Notificações locais na barra de status (RFM13).
 *
 * São disparadas pelo próprio app depois de sincronizar, e não por push: o
 * projeto não usa Firebase, então o que fazemos é avisar o aluno quando a
 * sincronização traz algo novo — comunicado, curso novo, certificado emitido.
 */
public final class Notificador {

    public static final String CANAL_ID = "proxima_etapa_avisos";

    private Notificador() {
    }

    /**
     * O canal precisa existir antes de qualquer notificação no Android 8+.
     * Criar de novo um canal que já existe não causa problema.
     */
    public static void criarCanal(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    "Avisos da Próxima Etapa",
                    NotificationManager.IMPORTANCE_DEFAULT);
            canal.setDescription("Comunicados, novos cursos, lembretes de encontro e certificados.");

            NotificationManager gerenciador = contexto.getSystemService(NotificationManager.class);
            if (gerenciador != null) gerenciador.createNotificationChannel(canal);
        }
    }

    /** Diz se o app pode notificar (no Android 13+ o usuário precisa autorizar). */
    public static boolean podeNotificar(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(contexto).areNotificationsEnabled();
    }

    /**
     * Mostra um aviso. Tocar nele abre a tela de notificações do app.
     *
     * Se o aluno não deu permissão, simplesmente não acontece nada — o aviso
     * continua disponível dentro do app, que é onde ele realmente mora.
     */
    public static void avisar(Context contexto, int id, String titulo, String corpo) {
        if (!podeNotificar(contexto)) return;

        Intent intent = new Intent(contexto, NotificacoesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent aoTocar = PendingIntent.getActivity(
                contexto, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder aviso = new NotificationCompat.Builder(contexto, CANAL_ID)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(titulo)
                .setContentText(corpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(corpo))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(aoTocar)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(contexto).notify(id, aviso.build());
        } catch (SecurityException e) {
            // A permissão pode ter sido revogada entre a checagem e o envio.
        }
    }
}

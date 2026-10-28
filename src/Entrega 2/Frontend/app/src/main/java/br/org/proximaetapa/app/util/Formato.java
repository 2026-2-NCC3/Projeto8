package br.org.proximaetapa.app.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Converte as datas que vêm da API (formato ISO, pensado para máquina) no
 * formato que o aluno lê (dd/MM). Fica em um lugar só para todas as telas
 * mostrarem data do mesmo jeito.
 */
public final class Formato {

    private static final Locale BR = new Locale("pt", "BR");

    private Formato() {
    }

    /** "2026-09-04" vira "04/09/2026". */
    public static String data(String iso) {
        if (iso == null || iso.length() < 10) return iso == null ? "" : iso;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", BR).parse(iso.substring(0, 10));
            return d == null ? iso : new SimpleDateFormat("dd/MM/yyyy", BR).format(d);
        } catch (ParseException e) {
            return iso;
        }
    }

    /** "2026-09-04" vira "04/09" — para listas, onde o ano ocupa espaço à toa. */
    public static String diaMes(String iso) {
        if (iso == null || iso.length() < 10) return iso == null ? "" : iso;
        return iso.substring(8, 10) + "/" + iso.substring(5, 7);
    }

    /** "2026-09-04 18:05:00" vira "04/09/2026 às 18:05". */
    public static String dataHora(String iso) {
        if (iso == null || iso.length() < 16) return data(iso);
        String base = iso.replace('T', ' ');
        return data(base.substring(0, 10)) + " às " + base.substring(11, 16);
    }

    /** Corta "18:00:00" em "18:00". */
    public static String hora(String hora) {
        if (hora == null) return "";
        return hora.length() >= 5 ? hora.substring(0, 5) : hora;
    }
}

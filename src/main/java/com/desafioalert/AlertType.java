package com.desafioalert;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tipos de aviso que se pueden lanzar con /aviso.
 * Cada tipo define su id (para el comando), el titulo que se muestra en el chat,
 * el color y el tono base de los sonidos de la animacion.
 */
public enum AlertType {
    INFO("info", "INFORMACIÓN", 0x55FF55, 1.0f),
    DIFICULTAD("dificultad", "CAMBIO DE DIFICULTAD", 0xFF8C1A, 0.7f),
    MOB("mob", "NUEVO MOB", 0xFF3B3B, 0.6f),
    JUGADOR("jugador", "AVISO", 0xFFE94D, 1.2f),
    MISION("mision", "MISIÓN DIARIA", 0x4D7DFF, 1.0f),
    EXPEDICION("expedicion", "EXPEDICIÓN", 0xFF7FD0, 1.3f),
    FINAL("final", "EVENTO ESPECIAL", 0xB040FF, 0.5f);

    public final String id;
    public final String label;
    public final int rgb;
    public final float pitch;

    AlertType(String id, String label, int rgb, float pitch) {
        this.id = id;
        this.label = label;
        this.rgb = rgb;
        this.pitch = pitch;
    }

    public static AlertType byId(String id) {
        if (id == null) {
            return null;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        for (AlertType type : values()) {
            if (type.id.equals(lower)) {
                return type;
            }
        }
        return null;
    }

    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (AlertType type : values()) {
            out.add(type.id);
        }
        return out;
    }
}

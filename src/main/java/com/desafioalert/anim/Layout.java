package com.desafioalert.anim;

/**
 * Distribuciones del fuego de pantalla (config "tamanoFuego").
 *
 * cell      = tamano de cada copia del sprite de fuego, relativo a 16 px.
 * bottomRow = fila de la textura donde "apoya" el fuego en el borde inferior.
 * copies    = desplazamientos horizontales (en unidades de 16 px) de cada copia.
 */
public enum Layout {
    A(0.8f, 12),
    B(0.35f, 16),
    C(0.25f, 16),
    D(0.5f, 16);

    final float cell;
    final int bottomRow;
    final float[] copies;

    Layout(float cell, int bottomRow) {
        this.cell = cell;
        this.bottomRow = bottomRow;
        if (cell >= 0.79f) {
            this.copies = new float[] {3.1f, -3.1f};
        } else {
            float step = 16.0f * cell * 0.8f;
            int n = (int) Math.ceil((double) (16.0f / step));
            float[] c = new float[n];
            for (int i = 0; i < n; i++) {
                c[i] = Math.round((((float) i - (float) (n - 1) / 2f) * step) / 0.05f) * 0.05f;
            }
            this.copies = c;
        }
    }

    public static Layout parse(String s) {
        if (s != null) {
            for (Layout l : values()) {
                if (l.name().equalsIgnoreCase(s.trim())) {
                    return l;
                }
            }
        }
        return D;
    }
}

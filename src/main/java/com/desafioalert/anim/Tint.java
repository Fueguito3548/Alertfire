package com.desafioalert.anim;

/**
 * Recolorea la textura de fuego de Minecraft (fire_1.png) hacia el color de cada tipo de aviso.
 *
 * Trabaja con pixeles ARGB. Agrupa los pixeles opacos por luminosidad en {@link #LEVELS} niveles,
 * promedia el color de cada nivel y mueve su tono hacia el color objetivo, conservando
 * la saturacion y el brillo relativos del fuego original.
 */
public final class Tint {
    public static final int LEVELS = 12;
    /** Tono (0..1) medio del fuego original de Minecraft (~25 grados). */
    private static final double H_ORIG = 0.06944444444444445;

    public static final class Result {
        public final int[] argb;
        public final int[] palette;

        Result(int[] argb, int[] palette) {
            this.argb = argb;
            this.palette = palette;
        }
    }

    private Tint() {
    }

    /**
     * @param src  pixeles ARGB de la tira de fuego
     * @param w    ancho (no se usa en el calculo)
     * @param h    alto (no se usa en el calculo)
     * @param tint color objetivo RGB, o negativo para no teñir (se deja el fuego original)
     */
    public static Result tintStrip(int[] src, int w, int h, int tint) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int px : src) {
            if ((px >>> 24) < 128) {
                continue;
            }
            double l = lum(px);
            min = Math.min(min, l);
            max = Math.max(max, l);
        }
        if (min > max) {
            return new Result(src.clone(), new int[LEVELS]);
        }

        double range = Math.max(1e-6, max - min);
        long[] sumR = new long[LEVELS];
        long[] sumG = new long[LEVELS];
        long[] sumB = new long[LEVELS];
        int[] count = new int[LEVELS];
        int[] bin = new int[src.length];

        for (int i = 0; i < src.length; i++) {
            int px = src[i];
            if ((px >>> 24) < 128) {
                bin[i] = -1;
                continue;
            }
            int b = Math.min(LEVELS - 1, (int) (((lum(px) - min) / range) * LEVELS));
            bin[i] = b;
            sumR[b] += (px >> 16) & 255;
            sumG[b] += (px >> 8) & 255;
            sumB[b] += px & 255;
            count[b]++;
        }

        int[][] avg = new int[LEVELS][3];
        for (int i = 0; i < LEVELS; i++) {
            if (count[i] > 0) {
                avg[i][0] = (int) Math.round((double) sumR[i] / (double) count[i]);
                avg[i][1] = (int) Math.round((double) sumG[i] / (double) count[i]);
                avg[i][2] = (int) Math.round((double) sumB[i] / (double) count[i]);
            } else if (i > 0) {
                avg[i] = avg[i - 1].clone();
            }
        }

        int[] palette = new int[LEVELS];
        for (int i = 0; i < LEVELS; i++) {
            palette[i] = 0xFF000000 | tintColor(avg[i][0], avg[i][1], avg[i][2], tint);
        }

        int[] out = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = bin[i] < 0 ? 0 : palette[bin[i]];
        }
        return new Result(out, palette);
    }

    private static double lum(int rgb) {
        return 0.299 * ((rgb >> 16) & 255) + 0.587 * ((rgb >> 8) & 255) + 0.114 * (rgb & 255);
    }

    static int tintColor(int r, int g, int b, int tint) {
        double rd = r / 255.0;
        double gd = g / 255.0;
        double bd = b / 255.0;
        if (tint < 0) {
            return pack(rd, gd, bd);
        }

        double[] target = rgbToHsv(((tint >> 16) & 255) / 255.0, ((tint >> 8) & 255) / 255.0, (tint & 255) / 255.0);
        double[] src = rgbToHsv(rd, gd, bd);

        // Diferencia de tono respecto al fuego original, en el rango -0.5..0.5
        double diff = (src[0] - H_ORIG) + 0.5;
        diff = diff - Math.floor(diff);
        double delta = diff - 0.5;

        double hue = target[0] + delta * 0.45;
        hue = hue - Math.floor(hue);

        double[] out = hsvToRgb(hue, src[1], src[2]);

        // Si el nuevo color queda mas oscuro que el original, se compensa el brillo (max x1.4)
        double lumSrc = 0.299 * rd + 0.587 * gd + 0.114 * bd;
        double lumNew = 0.299 * out[0] + 0.587 * out[1] + 0.114 * out[2];
        if (lumNew > 0.0 && lumNew < lumSrc) {
            double k = Math.min(1.4, Math.pow(lumSrc / lumNew, 0.6));
            out[0] = Math.min(1.0, out[0] * k);
            out[1] = Math.min(1.0, out[1] * k);
            out[2] = Math.min(1.0, out[2] * k);
        }
        return pack(out[0], out[1], out[2]);
    }

    private static int pack(double r, double g, double b) {
        int ri = (int) Math.round(r * 255.0);
        int gi = (int) Math.round(g * 255.0);
        int bi = (int) Math.round(b * 255.0);
        return (ri << 16) | (gi << 8) | bi;
    }

    static double[] rgbToHsv(double r, double g, double b) {
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double v = max;
        if (min == max) {
            return new double[] {0.0, 0.0, v};
        }
        double s = (max - min) / max;
        double rc = (max - r) / (max - min);
        double gc = (max - g) / (max - min);
        double bc = (max - b) / (max - min);
        double h;
        if (r == max) {
            h = bc - gc;
        } else if (g == max) {
            h = 2.0 + rc - bc;
        } else {
            h = 4.0 + gc - rc;
        }
        h = (h / 6.0) % 1.0;
        if (h < 0.0) {
            h += 1.0;
        }
        return new double[] {h, s, v};
    }

    static double[] hsvToRgb(double h, double s, double v) {
        if (s == 0.0) {
            return new double[] {v, v, v};
        }
        int i = (int) Math.floor(h * 6.0);
        double f = h * 6.0 - i;
        double p = v * (1.0 - s);
        double q = v * (1.0 - s * f);
        double t = v * (1.0 - s * (1.0 - f));
        i = ((i % 6) + 6) % 6;
        switch (i) {
            case 0:
                return new double[] {v, t, p};
            case 1:
                return new double[] {q, v, p};
            case 2:
                return new double[] {p, v, t};
            case 3:
                return new double[] {p, q, v};
            case 4:
                return new double[] {t, p, v};
            default:
                return new double[] {v, p, q};
        }
    }
}

package com.desafioalert.anim;

import com.desafioalert.AlertType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * La animacion del aviso. No depende de Minecraft: genera "comandos de dibujo"
 * (rectangulos y sprites de fuego) a traves de un {@link Sink}, que el cliente
 * luego pinta en pantalla.
 *
 * Linea de tiempo (a 20 "frames" por segundo):
 *   0.3s - 7.3s  fuegos artificiales de intro (cada vez mas seguidos)
 *   7.5s         explosion (BOOM)
 *   7.5s - 14.5s fuego de pantalla que sube desde abajo, brasas y destello
 *   9.2s         mensaje en el chat
 */
public final class Anim {
    public static final float FPS = 20.0f;
    /** Frame en el que ocurre la explosion final (7.5 s * 20). */
    public static final float FINALE = 150.0f;
    /** El fuego aparece 20 frames despues de la explosion... */
    public static final int FIRE_START = 20;
    /** ...tarda 14 frames en subir... */
    public static final int FIRE_IN = 14;
    /** ...empieza a bajar en el frame 124... */
    public static final int FIRE_OUT_START = 124;
    /** ...y tarda 14 frames en bajar. */
    public static final int FIRE_OUT = 14;
    private static final float FLASH_ALPHA = 0.85f;

    public static final int[] INTRO_COLORS = {
        0x4D7DFF, 0xFF7FD0, 0xB040FF, 0xFFE94D, 0xFF3B3B, 0x55FF55, 0xFF8C1A
    };

    /** Sprite 5x5 de cada chispa: D = oscuro, C = medio, W = claro, . = vacio. */
    private static final String[] SPRITE = {
        "..D..",
        ".DCD.",
        "DCWCD",
        ".DCD.",
        "..D.."
    };

    /** Escala mayor de 2 octavas (en semitonos). Con notePitch() va de 0.5 (grave) a 2.0 (agudo). */
    private static final int[] SCALE = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24};

    /** Destino del dibujo. Las coordenadas son pixeles de pantalla. */
    public interface Sink {
        void rect(int x, int y, int w, int h, int argb);

        void fire(int frame, int x, int y, int w, int h);
    }

    public enum CueKind {
        /** Explosion de fuego artificial de la intro. */
        SPARK,
        /** Explosion final. */
        BOOM,
        /** Aparece el fuego de pantalla (y el borde de color). */
        FIRE,
        /** Sale el mensaje en el chat (y el titulo en pantalla). */
        MESSAGE,
        /** Nota de "carga": una por cada tanda de fuegos artificiales, de grave a agudo. */
        CHARGE,
        /** Arranca la musica de fondo (una pista que dura toda la animacion). */
        MUSIC,
        /** Remate sonoro propio de cada tipo de aviso, junto a la explosion final. */
        STINGER
    }

    /** Momento (en segundos) en el que debe sonar un sonido o salir el mensaje. */
    public static final class Cue implements Comparable<Cue> {
        public final float time;
        public final CueKind kind;
        /** Tono del sonido (0.5 = una octava mas grave, 2.0 = una octava mas aguda). */
        public final float pitch;
        /** Multiplicador de volumen. */
        public final float gain;

        Cue(float time, CueKind kind) {
            this(time, kind, 1f, 1f);
        }

        Cue(float time, CueKind kind, float pitch, float gain) {
            this.time = time;
            this.kind = kind;
            this.pitch = pitch;
            this.gain = gain;
        }

        @Override
        public int compareTo(Cue other) {
            return Float.compare(this.time, other.time);
        }
    }

    private static final class Spark {
        float cx;
        float cz;
        float f0;
        float lf;
        float vx;
        float vz;
        float size;
        float grav;
        float drag;
        int[] pal;
    }

    private static final class Ember {
        float f0;
        float lf;
        float x0;
        float z0;
        float rise;
        float drift;
        int color;
    }

    private final AlertType type;
    private final Layout layout;
    private final int fireFrames;
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Ember> embers = new ArrayList<>();
    private final List<Cue> cues = new ArrayList<>();
    private final float slide;
    private final float heightScale;
    private final int bottomRow;
    private final float[] tmp = new float[3];

    /**
     * @param palette    paleta de 12 colores del fuego teñido (puede ser null)
     * @param lowerRows  cuantas filas de la textura se baja el fuego (config "bajarFuego")
     * @param seed       semilla del azar (cada aviso sale distinto)
     */
    public Anim(AlertType type, Layout layout, int fireFrames, int[] palette, float heightScale, int lowerRows, long seed) {
        this.type = type;
        this.layout = layout;
        this.fireFrames = Math.max(1, fireFrames);
        this.heightScale = Math.max(0.05f, heightScale);
        this.bottomRow = Math.max(1, layout.bottomRow - Math.max(0, lowerRows));
        this.slide = (float) this.bottomRow * layout.cell * this.heightScale + 0.3f;

        Random rng = new Random(seed);

        // Fuegos artificiales de intro: cada vez mas seguidos y de mas en mas a la vez.
        float t = 0.3f;
        float gap = 1f;
        while (t < 7.3f) {
            int n = gap > 0.5f ? 1 : (gap > 0.25f ? 2 : 3);
            for (int i = 0; i < n; i++) {
                int[] pal = spritePal(INTRO_COLORS[rng.nextInt(INTRO_COLORS.length)], false);
                burst(rng, uni(rng, -7.0f, 7.0f), uni(rng, -3.6f, 3.6f), t * FPS, pal,
                        18 + rng.nextInt(9), 6.0f, 11.0f, 0.75f, 26.0f, 2.5f, 1.6f);
                cues.add(new Cue(t, CueKind.SPARK));
            }
            // Nota de "carga": sube de grave a agudo a medida que avanza la intro.
            cues.add(new Cue(t, CueKind.CHARGE, notePitch((int) (t / 7.3f * 14f)), 1f));
            gap = Math.max(0.1f, gap * 0.9f);
            t += gap;
        }

        // Explosion final con el color del tipo de aviso.
        int[] boomPal = spritePal(type.rgb, true);
        burst(rng, 0f, 0.2f, FINALE, boomPal, 60, 8.0f, 15.0f, 1f, 60.0f, 1.5f, 1.4f);
        burst(rng, 0f, 0.2f, FINALE, boomPal, 24, 3.0f, 7.0f, 1.5f, 45.0f, 1.5f, 1.4f);
        cues.add(new Cue(7.5f, CueKind.BOOM));
        cues.add(new Cue(8.5f, CueKind.FIRE));
        cues.add(new Cue(9.2f, CueKind.MESSAGE));

        // Remate sonoro propio del tipo de aviso (dura 3 s como maximo) junto a la explosion.
        cues.add(new Cue(7.5f, CueKind.STINGER));

        // Musica de fondo: una sola pista de 14.5 s sincronizada con la animacion
        // (crece de grave a agudo hasta la explosion y luego se calma).
        cues.add(new Cue(0.0f, CueKind.MUSIC));
        Collections.sort(cues);

        // Brasas que suben desde el fuego.
        int[] emberLevels = {4, 6, 8, 10};
        float base = Math.max(0.8f, (float) (this.bottomRow - 4) * layout.cell * this.heightScale);
        for (int i = 0; i < 36; i++) {
            Ember e = new Ember();
            e.f0 = FINALE + uni(rng, 34.0f, 116.0f);
            e.x0 = uni(rng, -6.5f, 6.5f);
            e.z0 = base + uni(rng, -1.5f, 0.3f);
            e.rise = uni(rng, 1.5f, 3.0f);
            e.drift = uni(rng, -0.5f, 0.5f);
            e.lf = uni(rng, 24.0f, 40.0f);
            int level = emberLevels[rng.nextInt(emberLevels.length)];
            e.color = (palette != null && palette.length > level)
                    ? palette[level]
                    : shade(type.rgb, 0.55f + 0.1f * (float) rng.nextInt(4));
            embers.add(e);
        }
    }

    /** Duracion total en segundos. */
    public float duration() {
        return 14.5f;
    }

    public List<Cue> cues() {
        return cues;
    }

    /**
     * Genera los comandos de dibujo de un instante.
     *
     * @param seconds segundos desde el inicio de la animacion
     * @param w       ancho de pantalla en pixeles
     * @param h       alto de pantalla en pixeles
     */
    public void frame(float seconds, int w, int h, Sink sink) {
        float f = seconds * FPS;
        float k = (float) w / 16.0f;
        drawFire(sink, f - FINALE, w, h, k);
        drawEmbers(sink, f, w, h, k);
        for (Spark s : sparks) {
            drawSpark(sink, s, f, w, h, k);
        }
        drawFlash(sink, f - FINALE, w, h, k);
    }

    private void drawFire(Sink sink, float f, int w, int h, float k) {
        if (f < FIRE_START || f >= FIRE_OUT_START + FIRE_OUT + 1) {
            return;
        }
        float offset = 0f;
        if (f < FIRE_START + FIRE_IN) {
            float t = clamp((f - FIRE_START) / FIRE_IN);
            float u = 1f - t;
            offset = -this.slide * u * u * u;
        } else if (f > FIRE_OUT_START) {
            float t = clamp((f - FIRE_OUT_START) / FIRE_OUT);
            offset = -this.slide * t * t;
        }

        float local = f - FIRE_START;
        float sway = 0.15f * (float) Math.sin((Math.PI * 2.0 * (double) local) / 90.0);
        float bob = 0.1f * (float) Math.sin((Math.PI * 2.0 * (double) local) / 55.0);

        int frame = ((int) Math.floor((double) local)) % this.fireFrames;
        int cellW = Math.max(1, Math.round(this.layout.cell * k));
        int cellH = Math.max(1, Math.round((float) cellW * this.heightScale));
        int pw = 16 * cellW;
        int ph = 16 * cellH;
        int y = Math.round((float) (h - this.bottomRow * cellH) - (offset + bob) * k);

        for (int i = this.layout.copies.length - 1; i >= 0; i--) {
            int x = Math.round(((float) w / 2f + (this.layout.copies[i] + sway) * k) - (float) pw / 2f);
            sink.fire(frame, x, y, pw, ph);
        }
    }

    private void drawEmbers(Sink sink, float f, int w, int h, float k) {
        for (Ember e : this.embers) {
            float age = f - e.f0;
            if (age < -1.0f || age > e.lf) {
                continue;
            }
            float x;
            float z;
            float size;
            if (age < 0f) {
                x = e.x0;
                z = e.z0;
                size = 0.2f * (age + 1f);
            } else {
                float p = age / e.lf;
                x = e.x0 + e.drift * p;
                z = e.z0 + e.rise * p;
                size = p < 0.8333f ? 0.2f : (0.2f * (1f - p)) / 0.1667f;
            }
            int s = Math.round(size * k);
            if (s < 1) {
                continue;
            }
            int rx = Math.round(((float) w / 2f + x * k) - (float) s / 2f);
            int ry = Math.round(((float) h - z * k) - (float) s / 2f);
            sink.rect(rx, ry, s, s, e.color);
        }
    }

    private void burst(Random rng, float cx, float cz, float f0, int[] pal, int count,
                       float vMin, float vMax, float size, float life, float grav, float drag) {
        for (int i = 0; i < count; i++) {
            Spark s = new Spark();
            double angle = rng.nextDouble() * Math.PI * 2.0;
            float speed = uni(rng, vMin, vMax);
            s.vx = (float) Math.cos(angle) * speed;
            s.vz = (float) Math.sin(angle) * speed;
            s.lf = life * uni(rng, 0.75f, 1.1f);
            s.cx = cx;
            s.cz = cz;
            s.f0 = f0;
            s.size = size;
            s.grav = grav;
            s.drag = drag;
            s.pal = pal;
            this.sparks.add(s);
        }
    }

    /** Calcula la posicion (tmp[0], tmp[1]) y el tamano (tmp[2]) de una chispa en el paso i (0..12) de su vida. */
    private void node(Spark s, int i) {
        float t = (((float) i / 12.0f) * s.lf) / 20.0f;
        float d = (1f - (float) Math.exp((double) (-s.drag * t))) / s.drag;
        this.tmp[0] = s.cx + s.vx * d;
        this.tmp[1] = (s.cz + s.vz * d) - ((0.5f * s.grav) * t) * t;
        float size = s.size * (float) Math.pow((double) (1f - (float) i / 12.0f), 0.7);
        if (i <= 1) {
            size *= 1.35f;
        } else if (i % 2 == 1) {
            size *= 0.72f;
        }
        this.tmp[2] = size;
    }

    private void drawSpark(Sink sink, Spark s, float f, int w, int h, float k) {
        float age = f - s.f0;
        if (age < -1.0f || age > s.lf) {
            return;
        }
        float x;
        float z;
        float size;
        if (age < 0f) {
            node(s, 0);
            x = this.tmp[0];
            z = this.tmp[1];
            size = this.tmp[2] * (age + 1f);
        } else {
            float pos = (age / s.lf) * 12.0f;
            int i = (int) pos;
            if (i >= 12) {
                return;
            }
            float frac = pos - (float) i;
            node(s, i);
            float x0 = this.tmp[0];
            float z0 = this.tmp[1];
            float s0 = this.tmp[2];
            node(s, i + 1);
            x = x0 + (this.tmp[0] - x0) * frac;
            z = z0 + (this.tmp[1] - z0) * frac;
            size = s0 + (this.tmp[2] - s0) * frac;
        }

        float px = (0.2f * size) * k;
        if (px < 0.5f) {
            return;
        }
        if (px < 1f) {
            px = 1f;
        }
        float cx = (float) w / 2f + x * k;
        float cy = (float) h / 2f - z * k;
        float ox = cx - 2.5f * px;
        float oy = cy - 2.5f * px;

        for (int row = 0; row < 5; row++) {
            String line = SPRITE[row];
            for (int col = 0; col < 5; col++) {
                char c = line.charAt(col);
                if (c == '.') {
                    continue;
                }
                int x0 = Math.round(ox + (float) col * px);
                int x1 = Math.round(ox + (float) (col + 1) * px);
                int y0 = Math.round(oy + (float) row * px);
                int y1 = Math.round(oy + (float) (row + 1) * px);
                if (x1 <= x0 || y1 <= y0) {
                    continue;
                }
                sink.rect(x0, y0, x1 - x0, y1 - y0, s.pal[c == 'D' ? 0 : (c == 'C' ? 1 : 2)]);
            }
        }
    }

    /** Destello circular de color en el momento de la explosion. */
    private void drawFlash(Sink sink, float f, int w, int h, float k) {
        if (f <= -1.0f || f >= 7.0f) {
            return;
        }
        float r;
        if (f < 0f) {
            r = f + 1f;
        } else if (f < 2f) {
            r = 1f + (f / 2f) * 7.0f;
        } else {
            r = 8.0f - ((f - 2f) / 5.0f) * 8.0f;
        }
        float radius = (0.5f * r) * k;
        int step = Math.max(1, Math.round(0.05f * k));
        int cx = Math.round((float) w / 2f);
        int cy = Math.round(((float) h / 2f) - 0.2f * k);
        int color = ((int) (FLASH_ALPHA * 255f) << 24) | this.type.rgb;

        for (float yy = -radius; yy < radius; yy += (float) step) {
            float mid = yy + (float) step / 2f;
            float half = (float) Math.sqrt((double) Math.max(0f, radius * radius - mid * mid));
            int hw = Math.round(half / (float) step) * step;
            if (hw > 0) {
                sink.rect(cx - hw, cy + Math.round(yy), 2 * hw, step, color);
            }
        }
    }

    /** Tono (0.5 .. 2.0) del escalon i de la escala. */
    private static float notePitch(int step) {
        int i = Math.max(0, Math.min(SCALE.length - 1, step));
        return 0.5f * (float) Math.pow(2.0, SCALE[i] / 12.0);
    }

    private static float uni(Random rng, float min, float max) {
        return min + rng.nextFloat() * (max - min);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static int shade(int rgb, float factor) {
        int r = Math.min(255, Math.round((float) ((rgb >> 16) & 255) * factor));
        int g = Math.min(255, Math.round((float) ((rgb >> 8) & 255) * factor));
        int b = Math.min(255, Math.round((float) (rgb & 255) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Paleta de 3 tonos (oscuro, medio, claro) para el sprite de una chispa. */
    private static int[] spritePal(int rgb, boolean boom) {
        float r = (float) ((rgb >> 16) & 255) / 255.0f;
        float g = (float) ((rgb >> 8) & 255) / 255.0f;
        float b = (float) (rgb & 255) / 255.0f;
        if (boom) {
            return new int[] {
                argb(r * 0.5f, g * 0.5f, b * 0.5f),
                argb(r * 0.8f, g * 0.8f, b * 0.8f),
                argb(r, g, b)
            };
        }
        return new int[] {
            argb(r * 0.45f, g * 0.45f, b * 0.45f),
            argb(r, g, b),
            argb(r + (1f - r) * 0.85f, g + (1f - g) * 0.85f, b + (1f - b) * 0.85f)
        };
    }

    private static int argb(float r, float g, float b) {
        return 0xFF000000
                | (Math.round(r * 255.0f) << 16)
                | (Math.round(g * 255.0f) << 8)
                | Math.round(b * 255.0f);
    }
}

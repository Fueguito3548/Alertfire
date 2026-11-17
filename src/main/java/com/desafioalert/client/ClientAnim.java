package com.desafioalert.client;

import com.desafioalert.AlertConfig;
import com.desafioalert.AlertType;
import com.desafioalert.DesafioAlert;
import com.desafioalert.anim.Anim;
import com.desafioalert.anim.Layout;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.joml.Matrix4f;

/**
 * Lado cliente: cola de avisos, reproduccion de la animacion (sonidos + mensaje)
 * y dibujo en pantalla.
 */
public final class ClientAnim {
    /** Aviso recibido y todavia sin reproducir. */
    private static final class Pending {
        final AlertType type;
        final String message;
        final String[] soundIds;
        final float[] soundVolumes;

        Pending(AlertType type, String message, String[] soundIds, float[] soundVolumes) {
            this.type = type;
            this.message = message;
            this.soundIds = soundIds;
            this.soundVolumes = soundVolumes;
        }
    }

    /** Aviso que se esta reproduciendo ahora mismo. */
    private static final class Active {
        final AlertType type;
        final String message;
        final Anim anim;
        final FireTextures.Entry fire;
        final long startNanos;
        final String[] soundIds;
        final float[] soundVolumes;
        int nextCue;

        Active(AlertType type, String message, Anim anim, FireTextures.Entry fire,
               String[] soundIds, float[] soundVolumes) {
            this.soundIds = soundIds;
            this.soundVolumes = soundVolumes;
            this.type = type;
            this.message = message;
            this.anim = anim;
            this.fire = fire;
            this.startNanos = System.nanoTime();
        }
    }

    /**
     * Lista de comandos de dibujo reutilizable. Cada comando ocupa 6 enteros:
     * [tipo (0 = rect, 1 = fuego), a, b, c, d, e].
     */
    private static final class Commands implements Anim.Sink {
        int[] data = new int[24576];
        int count;

        void reset() {
            this.count = 0;
        }

        private int next() {
            if ((this.count + 1) * 6 > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            return 6 * this.count++;
        }

        @Override
        public void rect(int x, int y, int w, int h, int argb) {
            int o = next();
            this.data[o] = 0;
            this.data[o + 1] = x;
            this.data[o + 2] = y;
            this.data[o + 3] = w;
            this.data[o + 4] = h;
            this.data[o + 5] = argb;
        }

        @Override
        public void fire(int frame, int x, int y, int w, int h) {
            int o = next();
            this.data[o] = 1;
            this.data[o + 1] = frame;
            this.data[o + 2] = x;
            this.data[o + 3] = y;
            this.data[o + 4] = w;
            this.data[o + 5] = h;
        }
    }

    /** Borde de color de la pantalla: nace con el fuego y se apaga cuando el mensaje del chat desaparece. */
    private static final class Glow {
        final AlertType type;
        final long startNanos;

        Glow(AlertType type) {
            this.type = type;
            this.startNanos = System.nanoTime();
        }
    }

    /** Segundos que dura el borde: desde que sale el fuego (8.5 s) hasta que el chat oculta el mensaje (~19.2 s). */
    private static final float GLOW_SECONDS = 10.7f;
    private static final float GLOW_FADE_IN = 1.0f;
    private static final float GLOW_FADE_OUT = 1.2f;
    private static final int GLOW_STRIPS = 20;

    private static final ArrayDeque<Pending> QUEUE = new ArrayDeque<>();
    private static final Commands COMMANDS = new Commands();
    private static Active active;
    private static Glow glow;

    private ClientAnim() {
    }

    /** Llamado al recibir el paquete del servidor. Si ya hay un aviso en marcha, queda en cola. */
    public static void start(String typeId, String message, String[] soundIds, float[] soundVolumes) {
        AlertType type = AlertType.byId(typeId);
        if (type == null) {
            DesafioAlert.LOGGER.warn("Aviso con tipo desconocido: {}", typeId);
            return;
        }
        QUEUE.add(new Pending(type, message, soundIds, soundVolumes));
        if (active == null) {
            startNext();
        }
    }

    private static void startNext() {
        Pending next = QUEUE.poll();
        if (next == null) {
            active = null;
            return;
        }

        FireTextures.Entry fire = null;
        try {
            fire = FireTextures.get(next.type);
        } catch (Exception e) {
            DesafioAlert.LOGGER.error("Error preparando el fuego", e);
        }

        Layout layout = Layout.parse(AlertConfig.FIRE_LAYOUT.get());
        Anim anim = new Anim(
                next.type,
                layout,
                fire != null ? fire.frames() : 32,
                fire != null ? fire.palette : null,
                AlertConfig.FIRE_HEIGHT.get().floatValue(),
                AlertConfig.FIRE_LOWER.get(),
                System.nanoTime());
        active = new Active(next.type, next.message, anim, fire, next.soundIds, next.soundVolumes);
    }

    /** Se llama cada tick del cliente: dispara los sonidos/mensaje a su hora y pasa al siguiente aviso. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            active = null;
            glow = null;
            QUEUE.clear();
            return;
        }
        Active current = active;
        if (current == null) {
            return;
        }

        double elapsed = (double) (System.nanoTime() - current.startNanos) / 1.0E9;
        List<Anim.Cue> cues = current.anim.cues();
        while (current.nextCue < cues.size() && (double) cues.get(current.nextCue).time <= elapsed) {
            Anim.Cue cue = cues.get(current.nextCue);
            play(current, cue, current.nextCue);
            current.nextCue++;
        }
        if (elapsed > (double) current.anim.duration()) {
            startNext();
        }
    }

    private static void play(Active a, Anim.Cue cue, int index) {
        Minecraft mc = Minecraft.getInstance();
        switch (cue.kind) {
            case SPARK:
                // Tono ligeramente distinto en cada chispa
                sound(a, 0, 0.8f + 0.1f * (float) ((index * 37) % 7), 1f);
                break;
            case CHARGE:
                sound(a, 5, cue.pitch, cue.gain);
                break;
            case MUSIC:
                sound(a, 6, cue.pitch, cue.gain);
                break;
            case BOOM:
                sound(a, 1, a.type.pitch + 0.4f, 1f);
                sound(a, 2, a.type.pitch + 0.2f, 1f);
                break;
            case HIT:
                sound(a, 7, cue.pitch, cue.gain);
                break;
            case LONG:
                sound(a, 8, cue.pitch, cue.gain);
                break;
            case FIRE:
                sound(a, 3, a.type.pitch, 1f);
                glow = new Glow(a.type);
                break;
            case BLIP:
                sound(a, 4, cue.pitch, cue.gain);
                break;
            case MESSAGE:
                sound(a, 4, cue.pitch, cue.gain);
                Style style = Style.EMPTY.withColor(a.type.rgb);
                MutableComponent line = Component.literal("[" + a.type.label + "] ")
                        .setStyle(style.withBold(true))
                        .append(Component.literal(a.message).setStyle(style));
                mc.gui.getChat().addMessage(line);
                break;
            default:
                break;
        }
    }

    private static void sound(Active a, int index, float pitch, float gain) {
        float volume = a.soundVolumes[index] * AlertConfig.VOLUME.get().floatValue() * gain;
        if (volume <= 0f) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(a.soundIds[index].trim());
        if (id == null) {
            return;
        }
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }

    /**
     * Agrega a la lista de dibujo un difuminado de color en el borde de la pantalla:
     * marcos concentricos cuya opacidad baja hacia el centro.
     */
    private static void addGlow(int width, int height) {
        Glow g = glow;
        if (g == null) {
            return;
        }
        float u = (float) ((double) (System.nanoTime() - g.startNanos) / 1.0E9);
        if (u > GLOW_SECONDS) {
            glow = null;
            return;
        }
        float strength = AlertConfig.GLOW_INTENSITY.get().floatValue();
        if (strength <= 0f) {
            return;
        }
        float env = Math.min(Math.min(1f, u / GLOW_FADE_IN), Math.min(1f, (GLOW_SECONDS - u) / GLOW_FADE_OUT));
        env = env * env * (3f - 2f * env);

        int band = Math.max(GLOW_STRIPS, Math.round(Math.min(width, height) * 0.14f));
        int s = Math.max(1, band / GLOW_STRIPS);
        for (int i = 0; i < GLOW_STRIPS; i++) {
            float fall = 1f - (float) i / (float) GLOW_STRIPS;
            fall = fall * fall;
            int alpha = Math.round(255f * strength * env * fall);
            if (alpha <= 0) {
                continue;
            }
            int color = (alpha << 24) | g.type.rgb;
            int o = i * s;
            int innerW = width - 2 * o;
            int innerH = height - 2 * o - 2 * s;
            if (innerW <= 0 || innerH <= 0) {
                break;
            }
            COMMANDS.rect(o, o, innerW, s, color);
            COMMANDS.rect(o, height - o - s, innerW, s, color);
            COMMANDS.rect(o, o + s, s, innerH, color);
            COMMANDS.rect(width - o - s, o + s, s, innerH, color);
        }
    }

    /** Llamado por el overlay de la GUI en cada frame de render. */
    public static void render(GuiGraphics graphics, int width, int height) {
        COMMANDS.reset();
        addGlow(width, height);

        Active current = active;
        if (current != null) {
            float t = (float) ((double) (System.nanoTime() - current.startNanos) / 1.0E9);
            if (t <= current.anim.duration()) {
                current.anim.frame(t, width, height, COMMANDS);
            }
        }
        if (COMMANDS.count == 0) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int[] data = COMMANDS.data;
        int count = COMMANDS.count;
        int i = 0;
        while (i < count) {
            if (data[6 * i] == 0) {
                // Agrupa los rectangulos consecutivos en un solo batch
                int j = i;
                while (j < count && data[6 * j] == 0) {
                    j++;
                }
                drawRects(graphics, data, i, j);
                i = j;
            } else {
                if (current != null && current.fire != null) {
                    int w = current.fire.width;
                    graphics.blit(current.fire.location,
                            data[6 * i + 2], data[6 * i + 3], data[6 * i + 4], data[6 * i + 5],
                            0f, (float) (data[6 * i + 1] * w),
                            w, w, w, current.fire.height);
                }
                i++;
            }
        }
        RenderSystem.disableBlend();
    }

    private static void drawRects(GuiGraphics graphics, int[] data, int from, int to) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();
        for (int i = from; i < to; i++) {
            int o = 6 * i;
            float x0 = (float) data[o + 1];
            float y0 = (float) data[o + 2];
            float x1 = x0 + (float) data[o + 3];
            float y1 = y0 + (float) data[o + 4];
            int argb = data[o + 5];
            int a = (argb >>> 24) & 255;
            int r = (argb >> 16) & 255;
            int g = (argb >> 8) & 255;
            int b = argb & 255;
            buffer.vertex(matrix, x0, y1, 0f).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x1, y1, 0f).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x1, y0, 0f).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x0, y0, 0f).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
    }
}

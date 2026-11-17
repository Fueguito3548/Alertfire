package com.desafioalert.client;

import com.desafioalert.AlertType;
import com.desafioalert.DesafioAlert;
import com.desafioalert.anim.Tint;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Genera, a partir de la textura de fuego vanilla (fire_1.png), una version
 * teñida para cada tipo de aviso y la registra como textura dinamica.
 */
public final class FireTextures {
    public static final class Entry {
        public final ResourceLocation location;
        public final int[] palette;
        public final int width;
        public final int height;

        Entry(ResourceLocation location, int[] palette, int width, int height) {
            this.location = location;
            this.palette = palette;
            this.width = width;
            this.height = height;
        }

        /** Cantidad de frames de la tira (la textura es una columna de cuadrados). */
        public int frames() {
            return Math.max(1, this.height / this.width);
        }
    }

    private static final Map<AlertType, Entry> CACHE = new EnumMap<>(AlertType.class);
    private static int[] source;
    private static int srcW;
    private static int srcH;
    private static boolean failed;

    private FireTextures() {
    }

    public static Entry get(AlertType type) {
        Entry cached = CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        if (!loadSource()) {
            return null;
        }

        // "dificultad" conserva el fuego naranja original (sin teñir).
        int tint = type == AlertType.DIFICULTAD ? -1 : type.rgb;
        Tint.Result result = Tint.tintStrip(source, srcW, srcH, tint);

        NativeImage image = new NativeImage(srcW, srcH, false);
        for (int y = 0; y < srcH; y++) {
            for (int x = 0; x < srcW; x++) {
                int argb = result.argb[y * srcW + x];
                int a = argb >>> 24;
                int r = (argb >> 16) & 255;
                int g = (argb >> 8) & 255;
                int b = argb & 255;
                // NativeImage guarda ABGR
                image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }

        ResourceLocation location = new ResourceLocation(DesafioAlert.MODID, "dyn/fire_" + type.id);
        Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
        Entry entry = new Entry(location, result.palette, srcW, srcH);
        CACHE.put(type, entry);
        return entry;
    }

    /** Lee textures/block/fire_1.png del juego (una sola vez) y la guarda como ARGB. */
    private static boolean loadSource() {
        if (source != null) {
            return true;
        }
        if (failed) {
            return false;
        }
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                    .getResource(new ResourceLocation("minecraft", "textures/block/fire_1.png"));
            if (resource.isEmpty()) {
                throw new IllegalStateException("no existe textures/block/fire_1.png");
            }
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                srcW = image.getWidth();
                srcH = image.getHeight();
                int[] pixels = new int[srcW * srcH];
                for (int y = 0; y < srcH; y++) {
                    for (int x = 0; x < srcW; x++) {
                        int abgr = image.getPixelRGBA(x, y);
                        int a = abgr >>> 24;
                        int b = (abgr >> 16) & 255;
                        int g = (abgr >> 8) & 255;
                        int r = abgr & 255;
                        pixels[y * srcW + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
                source = pixels;
            }
            return true;
        } catch (Exception e) {
            failed = true;
            DesafioAlert.LOGGER.error("No se pudo leer la textura de fuego de Minecraft; el aviso saldra sin fuego", e);
            return false;
        }
    }
}

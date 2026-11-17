package com.desafioalert;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AlertConfig {
    /** Sonidos generales de la animacion (posiciones 0..6 de las listas de abajo). */
    public static final int BASE_SOUNDS = 7;
    /**
     * Total de sonidos: los generales + un "remate" por cada tipo de aviso
     * (posicion BASE_SOUNDS + AlertType.ordinal()). Hay 7 tipos.
     */
    public static final int SOUND_COUNT = BASE_SOUNDS + 7;

    // ---------------------------------------------------------------- COMMON (servidor)
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue FINAL_INVINCIBLE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FINAL_INVINCIBLE_DELAY;

    /** Claves de cada sonido en el archivo de config (mismo orden que las demas listas). */
    public static final String[] SOUND_KEYS = new String[SOUND_COUNT];
    public static final String[] SOUND_HELP = new String[SOUND_COUNT];
    public static final String[] SOUND_DEFAULTS = new String[SOUND_COUNT];
    public static final double[] SOUND_DEFAULT_VOLUMES = new double[SOUND_COUNT];

    @SuppressWarnings("unchecked")
    public static final ForgeConfigSpec.ConfigValue<String>[] SOUND_IDS = new ForgeConfigSpec.ConfigValue[SOUND_COUNT];
    public static final ForgeConfigSpec.DoubleValue[] SOUND_VOLUMES = new ForgeConfigSpec.DoubleValue[SOUND_COUNT];

    // ---------------------------------------------------------------- CLIENT
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue FIRE_HEIGHT;
    public static final ForgeConfigSpec.IntValue FIRE_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> FIRE_LAYOUT;
    public static final ForgeConfigSpec.DoubleValue VOLUME;
    public static final ForgeConfigSpec.DoubleValue GLOW_INTENSITY;
    public static final ForgeConfigSpec.BooleanValue SHOW_TITLE;

    private static void sound(int i, String key, String help, String id, double volume) {
        SOUND_KEYS[i] = key;
        SOUND_HELP[i] = help;
        SOUND_DEFAULTS[i] = id;
        SOUND_DEFAULT_VOLUMES[i] = volume;
    }

    static {
        // Los sonidos "desafioalert:..." son propios del mod (carpeta assets/desafioalert/sounds).
        sound(0, "fuegos_artificiales", "Explosion de cada fuego artificial de la intro (hay 4 variantes al azar).",
                "desafioalert:firework", 0.5);
        sound(1, "explosion_grande", "Explosion final (capa grave).", "desafioalert:boom", 0.9);
        sound(2, "explosion_destellos", "Explosion final (capa de destellos).", "desafioalert:sparkle_blast", 0.7);
        sound(3, "fuego_pantalla", "Cuando aparece el fuego de pantalla.", "desafioalert:fire", 0.7);
        sound(4, "mensaje_chat", "Cuando sale el mensaje en el chat.", "desafioalert:message", 0.7);
        sound(5, "notas_carga", "Nota que acompana cada tanda de fuegos artificiales; sube de grave a agudo (efecto 'cargando').",
                "desafioalert:charge", 0.5);
        sound(6, "musica_fondo", "Musica de fondo (una pista de 14.5 s que dura toda la animacion).",
                "desafioalert:music", 0.35);
        // Un remate distinto por cada tipo de aviso, junto a la explosion final (maximo ~3 s).
        for (AlertType type : AlertType.values()) {
            sound(BASE_SOUNDS + type.ordinal(), "remate_" + type.id,
                    "Sonido de remate del aviso '" + type.id + "' (suena junto a la explosion final).",
                    "desafioalert:final_" + type.id, type == AlertType.FINAL ? 1.0 : 0.9);
        }

        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();

        common.push("evento_especial");
        FINAL_INVINCIBLE_SECONDS = common
                .comment("Segundos de invencibilidad para todos los jugadores con el aviso de tipo 'final' (morado). 0 = desactivado.")
                .defineInRange("invencibleSegundos", 10, 0, 600);
        FINAL_INVINCIBLE_DELAY = common
                .comment("Segundos que tarda en empezar la invencibilidad desde que se manda el aviso (8.5 = justo cuando aparece el fuego).")
                .defineInRange("retrasoSegundos", 8.5, 0.0, 60.0);
        common.pop();

        common.comment(
                "Sonidos de la animacion. Por defecto son sonidos propios del mod (desafioalert:...).",
                "Puedes escribir el id de cualquier sonido de Minecraft (o de un mod / resource pack), por ejemplo:",
                "minecraft:entity.lightning_bolt.thunder, minecraft:entity.ender_dragon.growl, minecraft:block.bell.use",
                "Si el id no existe, ese sonido simplemente no suena. Para quitar un sonido pon volumen 0.");
        common.push("sonidos");
        for (int i = 0; i < SOUND_COUNT; i++) {
            common.comment(SOUND_HELP[i]);
            common.push(SOUND_KEYS[i]);
            SOUND_IDS[i] = common.define("sonido", SOUND_DEFAULTS[i]);
            SOUND_VOLUMES[i] = common.defineInRange("volumen", SOUND_DEFAULT_VOLUMES[i], 0.0, 2.0);
            common.pop();
        }
        common.pop();
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        FIRE_LAYOUT = client
                .comment("Tamano del fuego de pantalla: A (enorme), B (pequeno y fino), C (bajito), D (intermedio, recomendado).")
                .define("tamanoFuego", "D");
        FIRE_HEIGHT = client
                .comment("Altura del fuego: 1.0 = la original (muy alto), 0.5 = la mitad, 0.3 = bajito.")
                .defineInRange("alturaFuego", 0.5, 0.1, 1.5);
        FIRE_LOWER = client
                .comment("Cuanto bajar el fuego (en filas de la textura). Si ves huecos debajo del fuego, sube este numero.")
                .defineInRange("bajarFuego", 2, 0, 8);
        VOLUME = client
                .comment("Volumen de los sonidos de la animacion (1.0 = normal, 0 = silencio).")
                .defineInRange("volumen", 1.0, 0.0, 2.0);
        GLOW_INTENSITY = client
                .comment("Intensidad del difuminado de color en el borde de la pantalla cuando sale el fuego (0 = desactivado, 0.25 = sutil, 1 = fuerte).")
                .defineInRange("bordeIntensidad", 0.25, 0.0, 1.0);
        SHOW_TITLE = client
                .comment("Mostrar el titulo grande en pantalla (tipo de aviso + mensaje) cuando sale el mensaje.")
                .define("mostrarTitulo", true);
        CLIENT_SPEC = client.build();
    }

    private AlertConfig() {
    }
}

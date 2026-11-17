package com.desafioalert;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AlertConfig {
    /** Cantidad de sonidos configurables de la animacion. */
    public static final int SOUND_COUNT = 5;

    // ---------------------------------------------------------------- COMMON (servidor)
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue FINAL_INVINCIBLE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FINAL_INVINCIBLE_DELAY;

    /** Claves de cada sonido en el archivo de config (mismo orden que las listas de abajo). */
    public static final String[] SOUND_KEYS = {"chispas", "explosion", "explosion_capa2", "fuego", "mensaje"};
    public static final String[] SOUND_DEFAULTS = {
        "minecraft:entity.firework_rocket.blast",
        "minecraft:entity.generic.explode",
        "minecraft:entity.firework_rocket.large_blast",
        "minecraft:entity.blaze.shoot",
        "minecraft:ui.toast.challenge_complete"
    };
    public static final double[] SOUND_DEFAULT_VOLUMES = {0.35, 0.8, 0.9, 0.8, 0.7};
    @SuppressWarnings("unchecked")
    public static final ForgeConfigSpec.ConfigValue<String>[] SOUND_IDS = new ForgeConfigSpec.ConfigValue[SOUND_COUNT];
    public static final ForgeConfigSpec.DoubleValue[] SOUND_VOLUMES = new ForgeConfigSpec.DoubleValue[SOUND_COUNT];

    // ---------------------------------------------------------------- CLIENT
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue FIRE_HEIGHT;
    public static final ForgeConfigSpec.IntValue FIRE_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> FIRE_LAYOUT;
    public static final ForgeConfigSpec.DoubleValue VOLUME;

    static {
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
                "Sonidos de la animacion. Escribe el id de cualquier sonido de Minecraft (o de un mod / resource pack).",
                "Ejemplos: minecraft:entity.lightning_bolt.thunder, minecraft:entity.ender_dragon.growl,",
                "minecraft:entity.wither.spawn, minecraft:block.bell.use, minecraft:entity.player.levelup,",
                "minecraft:entity.firework_rocket.twinkle, minecraft:block.note_block.bell",
                "Si el id no existe, ese sonido simplemente no suena. Para quitar un sonido pon volumen 0.");
        common.push("sonidos");
        for (int i = 0; i < SOUND_COUNT; i++) {
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
        CLIENT_SPEC = client.build();
    }

    private AlertConfig() {
    }
}

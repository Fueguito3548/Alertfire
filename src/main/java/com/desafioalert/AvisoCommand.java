package com.desafioalert;

import com.desafioalert.net.AlertPacket;
import com.desafioalert.net.NetworkHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * /aviso <tipo> <mensaje>
 * Solo para operadores (nivel 2). Manda el aviso a todos los jugadores.
 */
@Mod.EventBusSubscriber(modid = DesafioAlert.MODID)
public final class AvisoCommand {
    private static final int MAX_LENGTH = 300;

    private AvisoCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("aviso")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("tipo", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(AlertType.ids(), builder))
                                .then(Commands.argument("mensaje", StringArgumentType.greedyString())
                                        .executes(AvisoCommand::run))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String typeId = StringArgumentType.getString(ctx, "tipo");
        AlertType type = AlertType.byId(typeId);
        if (type == null) {
            source.sendFailure(Component.literal(
                    "Tipo desconocido: '" + typeId + "'. Tipos: " + String.join(", ", AlertType.ids())));
            return 0;
        }

        String message = StringArgumentType.getString(ctx, "mensaje").trim();
        if (message.length() > MAX_LENGTH) {
            message = message.substring(0, MAX_LENGTH);
        }

        // Los sonidos salen de la config del servidor y se mandan a los clientes en el paquete.
        String[] soundIds = new String[AlertConfig.SOUND_COUNT];
        float[] soundVolumes = new float[AlertConfig.SOUND_COUNT];
        for (int i = 0; i < AlertConfig.SOUND_COUNT; i++) {
            soundIds[i] = AlertConfig.SOUND_IDS[i].get();
            soundVolumes[i] = AlertConfig.SOUND_VOLUMES[i].get().floatValue();
        }

        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new AlertPacket(type.id, message, soundIds, soundVolumes));
        DesafioAlert.LOGGER.info("[Aviso/{}] {}", type.id, message);

        if (type == AlertType.FINAL) {
            giveInvincibility(source.getServer());
        }
        return 1;
    }

    /** Resistencia V (= daño casi nulo) a todos los jugadores durante el evento especial. */
    private static void giveInvincibility(MinecraftServer server) {
        int ticks = AlertConfig.FINAL_INVINCIBLE_SECONDS.get() * 20;
        if (ticks <= 0) {
            return;
        }
        int delay = (int) Math.round(AlertConfig.FINAL_INVINCIBLE_DELAY.get() * 20.0);
        ServerTasks.schedule(delay, () -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 4, false, true, true));
            }
        });
    }
}

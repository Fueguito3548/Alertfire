package com.desafioalert.net;

import com.desafioalert.AlertConfig;
import com.desafioalert.client.ClientAnim;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquete servidor -> clientes con el aviso: tipo, mensaje y los 5 sonidos
 * (id + volumen) tomados de la config del servidor.
 */
public final class AlertPacket {
    public final String type;
    public final String message;
    public final String[] soundIds;
    public final float[] soundVolumes;

    public AlertPacket(String type, String message, String[] soundIds, float[] soundVolumes) {
        this.type = type;
        this.message = message;
        this.soundIds = soundIds;
        this.soundVolumes = soundVolumes;
    }

    public static void encode(AlertPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.type, 32);
        buf.writeUtf(packet.message, 1024);
        for (int i = 0; i < AlertConfig.SOUND_COUNT; i++) {
            buf.writeUtf(packet.soundIds[i], 256);
            buf.writeFloat(packet.soundVolumes[i]);
        }
    }

    public static AlertPacket decode(FriendlyByteBuf buf) {
        String type = buf.readUtf(32);
        String message = buf.readUtf(1024);
        String[] soundIds = new String[AlertConfig.SOUND_COUNT];
        float[] soundVolumes = new float[AlertConfig.SOUND_COUNT];
        for (int i = 0; i < AlertConfig.SOUND_COUNT; i++) {
            soundIds[i] = buf.readUtf(256);
            soundVolumes[i] = buf.readFloat();
        }
        return new AlertPacket(type, message, soundIds, soundVolumes);
    }

    public static void handle(AlertPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientAnim.start(packet.type, packet.message, packet.soundIds, packet.soundVolumes)));
        context.setPacketHandled(true);
    }
}

package com.desafioalert.net;

import com.desafioalert.DesafioAlert;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DesafioAlert.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private NetworkHandler() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, AlertPacket.class,
                AlertPacket::encode, AlertPacket::decode, AlertPacket::handle);
    }
}

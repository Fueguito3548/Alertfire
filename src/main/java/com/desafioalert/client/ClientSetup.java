package com.desafioalert.client;

import com.desafioalert.DesafioAlert;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DesafioAlert.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Se dibuja por encima de todo el HUD.
        event.registerAboveAll("aviso_animacion",
                (gui, graphics, partialTick, width, height) -> ClientAnim.render(graphics, width, height));
    }
}

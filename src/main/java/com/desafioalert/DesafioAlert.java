package com.desafioalert;

import com.desafioalert.net.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Desafio Alert: comando /aviso con animacion, color y sonido
 * para anunciar cambios de balance en el evento.
 */
@Mod(DesafioAlert.MODID)
public class DesafioAlert {
    public static final String MODID = "desafioalert";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DesafioAlert() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AlertConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AlertConfig.CLIENT_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}

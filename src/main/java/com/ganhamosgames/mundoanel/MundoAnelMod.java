package com.ganhamosgames.mundoanel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MundoAnelMod implements ModInitializer {
    public static final String MOD_ID = "mundo_anel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerChunkEvents.CHUNK_GENERATE.register(AnelWorldGenerator::generate);
        LOGGER.info("Mundo Anel carregado. O terreno especial e gerado em chunks novos ao redor de X=0, Z=0.");
    }
}

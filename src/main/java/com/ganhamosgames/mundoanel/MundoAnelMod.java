package com.ganhamosgames.mundoanel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MundoAnelMod implements ModInitializer {
    public static final String MOD_ID = "mundo_anel";
    public static final long RECOMMENDED_SEED = 202611081214L;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerChunkEvents.CHUNK_GENERATE.register(AnelWorldGenerator::generate);

        // Keep the world spawn on the safe central meadow, not at vanilla's random biome spawn.
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() != World.OVERWORLD) {
                return;
            }

            // At x=0,z=0 the generated center island has its surface at y=97.
            world.setSpawnPos(new BlockPos(0, 98, 0), 0.0F);
            LOGGER.info("Mundo Anel spawn fixed at 0,98,0. Recommended world seed: {}",
                    RECOMMENDED_SEED);
        });

        LOGGER.info("Mundo Anel carregado para Minecraft 1.21.4.");
    }
}

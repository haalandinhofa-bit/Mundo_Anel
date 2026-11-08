package com.ganhamosgames.mundoanel;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Objects;

/**
 * Generates the ring-shaped island after a new Overworld chunk has been created.
 * The rest of the world keeps the vanilla chunk generator.
 */
final class AnelWorldGenerator {
    private static final int INNER_RADIUS = 72;
    private static final int OUTER_RADIUS = 209;
    private static final int OUTER_RIVER_WIDTH = 32;
    private static final int LONG_RIVER_HALF_WIDTH = 11;
    private static final int RIVER_REACH = 238;
    private static final int MAX_REGION_RADIUS = OUTER_RADIUS + OUTER_RIVER_WIDTH + 24;
    private static final int TREE_GRID_X = 17;
    private static final int TREE_GRID_Z = 19;

    private AnelWorldGenerator() {
    }

    static void generate(ServerWorld world, WorldChunk chunk) {
        if (!Objects.equals(world.getRegistryKey(), World.OVERWORLD)) {
            return;
        }

        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        if (startX > MAX_REGION_RADIUS || startX + 15 < -MAX_REGION_RADIUS
                || startZ > MAX_REGION_RADIUS || startZ + 15 < -MAX_REGION_RADIUS) {
            return;
        }

        int seaLevel = world.getSeaLevel();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        boolean changed = false;

        for (int localX = 0; localX < 16; localX++) {
            int x = startX + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int z = startZ + localZ;
                MapSample sample = sampleAt(x, z, seaLevel);

                switch (sample.region()) {
                    case CENTER_ISLAND -> {
                        setLandColumn(world, chunk, pos, x, z, sample.surfaceY(),
                                Blocks.STONE.getDefaultState(), Blocks.STONE.getDefaultState());
                        changed = true;
                    }
                    case LAND -> {
                        SurfacePalette palette = paletteFor(sample.sector(), sample.surfaceY());
                        setLandColumn(world, chunk, pos, x, z, sample.surfaceY(), palette.top(), palette.filler());
                        changed = true;
                    }
                    case INNER_LAKE, OUTER_WATER, LONG_RIVER -> {
                        setWaterColumn(world, chunk, pos, x, z, seaLevel,
                                sample.region() == Region.LONG_RIVER);
                        changed = true;
                    }
                    case NORMAL -> {
                        // Preserve the vanilla-generated terrain outside the special region.
                    }
                }
            }
        }

        if (!changed) {
            return;
        }

        addVegetation(world, chunk, startX, startZ, seaLevel, pos);
        paintBiomes(world, chunk);
        chunk.refreshSurfaceY();
        chunk.markNeedsSaving();

        if (chunk.getPos().x == 0 && chunk.getPos().z == 0) {
            MundoAnelMod.LOGGER.info("Centro do Mundo Anel gerado em X=0, Z=0.");
        }
    }

    private static MapSample sampleAt(int x, int z, int seaLevel) {
        double dx = x;
        double dz = z;
        double radius = Math.sqrt(dx * dx + dz * dz);

        double inner = INNER_RADIUS
                + Math.sin(x * 0.018) * 4.0
                + Math.cos(z * 0.021) * 4.0;
        double outer = OUTER_RADIUS
                + Math.sin(x * 0.014) * 8.0
                + Math.cos(z * 0.019) * 6.0;
        double islandRadius = 18.0
                + Math.sin(x * 0.11 + z * 0.07) * 1.8;

        if (radius < islandRadius) {
            int centerY = 80 + (int) ((1.0 - radius / islandRadius) * 17.0)
                    + (int) (Math.sin(x * 0.22) * Math.cos(z * 0.19) * 2.0);
            return new MapSample(Region.CENTER_ISLAND, -1, centerY);
        }

        // The central lagoon is wide enough to read as an inland sea.
        if (radius < inner) {
            return new MapSample(Region.INNER_LAKE, -1, seaLevel);
        }

        double riverCenterZ = -66.0 + x * 0.085
                + Math.sin(x / 36.0) * 18.0
                + Math.sin(x / 12.5) * 5.0;
        double riverWidth = LONG_RIVER_HALF_WIDTH + 1.5 * (0.5 + Math.sin(x / 27.0));

        if (Math.abs(x) <= RIVER_REACH
                && Math.abs(z - riverCenterZ) <= riverWidth
                && radius < outer + OUTER_RIVER_WIDTH + 8.0) {
            return new MapSample(Region.LONG_RIVER, -1, seaLevel);
        }

        if (radius >= outer && radius <= outer + OUTER_RIVER_WIDTH) {
            return new MapSample(Region.OUTER_WATER, -1, seaLevel);
        }

        if (radius >= inner && radius < outer) {
            int sector = sectorAt(x, z);
            int height = terrainHeight(x, z, radius, inner, outer, sector, seaLevel, riverCenterZ, riverWidth);
            return new MapSample(Region.LAND, sector, height);
        }

        return new MapSample(Region.NORMAL, -1, seaLevel);
    }

    private static int sectorAt(int x, int z) {
        double angle = Math.atan2(z, x);
        angle += Math.sin(x * 0.015 + z * 0.012) * 0.10;
        int sector = (int) Math.floor((angle + Math.PI) / (Math.PI / 3.0));
        return Math.floorMod(sector, 6);
    }

    private static int terrainHeight(int x, int z, double radius, double inner, double outer,
                                     int sector, int seaLevel, double riverCenterZ, double riverWidth) {
        double rolling = Math.sin(x * 0.031) * 3.2
                + Math.cos(z * 0.027) * 3.4
                + Math.sin((x + z) * 0.019) * 2.8;

        double base = switch (sector) {
            case 0 -> 88.0 + rolling + Math.pow(Math.max(0.0,
                    Math.sin(x * 0.021) * Math.cos(z * 0.018)), 2.0) * 42.0;
            case 1 -> 82.0 + rolling + Math.sin(z * 0.045) * 3.0;
            case 2 -> 79.0 + rolling + Math.cos(x * 0.041) * 2.5;
            case 3 -> 77.0 + rolling * 0.75;
            case 4 -> 86.0 + rolling + Math.sin((x - z) * 0.028) * 4.0;
            default -> 74.0 + rolling * 0.6 + Math.sin(z * 0.052) * 4.0;
        };

        double coast = clamp(Math.min(radius - inner, outer - radius) / 28.0, 0.0, 1.0);
        double height = (seaLevel + 1.0) + coast * (base - (seaLevel + 1.0));

        double riverDistance = Math.abs(z - riverCenterZ);
        if (riverDistance < riverWidth + 14.0) {
            double bank = clamp((riverDistance - riverWidth) / 14.0, 0.0, 1.0);
            height = Math.min(height, seaLevel + 1.0 + bank * 11.0);
        }

        return (int) Math.round(clamp(height, seaLevel + 1.0, 132.0));
    }

    private static SurfacePalette paletteFor(int sector, int height) {
        return switch (sector) {
            case 0 -> new SurfacePalette(
                    height > 99 ? Blocks.SNOW_BLOCK.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState(),
                    height > 99 ? Blocks.STONE.getDefaultState() : Blocks.DIRT.getDefaultState());
            case 1 -> new SurfacePalette(Blocks.GRASS_BLOCK.getDefaultState(), Blocks.PODZOL.getDefaultState());
            case 2 -> new SurfacePalette(Blocks.GRASS_BLOCK.getDefaultState(), Blocks.DIRT.getDefaultState());
            case 3 -> new SurfacePalette(Blocks.GRASS_BLOCK.getDefaultState(), Blocks.COARSE_DIRT.getDefaultState());
            case 4 -> new SurfacePalette(Blocks.RED_SAND.getDefaultState(), Blocks.ORANGE_TERRACOTTA.getDefaultState());
            default -> new SurfacePalette(Blocks.SAND.getDefaultState(), Blocks.SANDSTONE.getDefaultState());
        };
    }

    private static void setLandColumn(ServerWorld world, WorldChunk chunk, BlockPos.Mutable pos,
                                      int x, int z, int surfaceY, BlockState top, BlockState filler) {
        int minY = world.getBottomY();
        int currentTop = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;

        for (int y = currentTop; y > surfaceY; y--) {
            set(chunk, pos, x, y, z, Blocks.AIR.getDefaultState());
        }

        int stoneBase = surfaceY - 6;
        if (currentTop < stoneBase) {
            for (int y = Math.max(minY, currentTop + 1); y < stoneBase; y++) {
                set(chunk, pos, x, y, z, Blocks.STONE.getDefaultState());
            }
        }

        for (int y = Math.max(minY, surfaceY - 6); y < surfaceY; y++) {
            set(chunk, pos, x, y, z, y >= surfaceY - 3 ? filler : Blocks.STONE.getDefaultState());
        }
        set(chunk, pos, x, surfaceY, z, top);
    }

    private static void setWaterColumn(ServerWorld world, WorldChunk chunk, BlockPos.Mutable pos,
                                       int x, int z, int seaLevel, boolean river) {
        int minY = world.getBottomY();
        int currentTop = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
        int depth = 5 + Math.floorMod(x * 31 + z * 17, 4);
        int bedY = Math.max(minY + 8, seaLevel - depth);

        for (int y = currentTop; y > seaLevel; y--) {
            set(chunk, pos, x, y, z, Blocks.AIR.getDefaultState());
        }

        if (currentTop < bedY) {
            for (int y = Math.max(minY, currentTop + 1); y < bedY; y++) {
                set(chunk, pos, x, y, z, Blocks.STONE.getDefaultState());
            }
        }

        set(chunk, pos, x, bedY, z,
                river ? Blocks.GRAVEL.getDefaultState() : Blocks.SAND.getDefaultState());

        for (int y = bedY + 1; y <= seaLevel; y++) {
            set(chunk, pos, x, y, z, Blocks.WATER.getDefaultState());
        }
    }

    private static void set(WorldChunk chunk, BlockPos.Mutable pos,
                            int x, int y, int z, BlockState state) {
        pos.set(x, y, z);
        chunk.setBlockState(pos, state, false);
    }

    private static void addVegetation(ServerWorld world, WorldChunk chunk, int startX, int startZ,
                                      int seaLevel, BlockPos.Mutable pos) {
        for (int localX = 2; localX <= 13; localX++) {
            int x = startX + localX;
            for (int localZ = 2; localZ <= 13; localZ++) {
                int z = startZ + localZ;
                MapSample sample = sampleAt(x, z, seaLevel);
                if (sample.region() != Region.LAND) {
                    continue;
                }

                int sector = sample.sector();
                long hash = coordinateHash(x, z);
                if ((sector == 1 || sector == 2 || sector == 3)
                        && Math.floorMod(x, TREE_GRID_X) == 8
                        && Math.floorMod(z, TREE_GRID_Z) == 9
                        && Math.floorMod(hash, 100) < 68) {
                    placeTree(chunk, pos, localX, sample.surfaceY() + 1, localZ, sector, hash);
                } else if (sector == 5
                        && Math.floorMod(x, 23) == 11
                        && Math.floorMod(z, 23) == 11
                        && Math.floorMod(hash, 100) < 22) {
                    int cactusHeight = 1 + (int) Math.floorMod(hash >>> 3, 3);
                    for (int dy = 0; dy < cactusHeight; dy++) {
                        set(chunk, pos, x, sample.surfaceY() + 1 + dy, z, Blocks.CACTUS.getDefaultState());
                    }
                }
            }
        }
    }

    private static void placeTree(WorldChunk chunk, BlockPos.Mutable pos, int localX, int baseY,
                                  int localZ, int sector, long hash) {
        int height = 5 + (int) Math.floorMod(hash >>> 4, 3);
        BlockState log = sector == 3 ? Blocks.ACACIA_LOG.getDefaultState()
                : (sector == 1 ? Blocks.SPRUCE_LOG.getDefaultState() : Blocks.OAK_LOG.getDefaultState());
        BlockState leaves = sector == 3 ? Blocks.ACACIA_LEAVES.getDefaultState()
                : (sector == 1 ? Blocks.SPRUCE_LEAVES.getDefaultState() : Blocks.OAK_LEAVES.getDefaultState());

        for (int dy = 0; dy < height; dy++) {
            set(chunk, pos, chunk.getPos().getStartX() + localX, baseY + dy,
                    chunk.getPos().getStartZ() + localZ, log);
        }

        int worldX = chunk.getPos().getStartX() + localX;
        int worldZ = chunk.getPos().getStartZ() + localZ;
        for (int dy = height - 3; dy <= height; dy++) {
            int radius = dy == height ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > (radius == 2 ? 3 : 2)) {
                        continue;
                    }
                    int lx = localX + dx;
                    int lz = localZ + dz;
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
                        continue;
                    }
                    int leafY = baseY + dy;
                    pos.set(worldX + dx, leafY, worldZ + dz);
                    if (chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, leaves, false);
                    }
                }
            }
        }
    }

    private static void paintBiomes(ServerWorld world, WorldChunk chunk) {
        Registry<Biome> registry = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
        BiomePalette palette = new BiomePalette(
                registry.getEntry(BiomeKeys.SNOWY_PLAINS.getValue()).orElseThrow(),
                registry.getEntry(BiomeKeys.TAIGA.getValue()).orElseThrow(),
                registry.getEntry(BiomeKeys.FOREST.getValue()).orElseThrow(),
                registry.getEntry(BiomeKeys.SAVANNA.getValue()).orElseThrow(),
                registry.getEntry(BiomeKeys.BADLANDS.getValue()).orElseThrow(),
                registry.getEntry(BiomeKeys.DESERT.getValue()).orElseThrow(),
                registry.getEntry(BiomeKeys.OCEAN.getValue()).orElseThrow(),
                registry.getEntry(BiomeKeys.RIVER.getValue()).orElseThrow(),
                registry.getEntry(BiomeKeys.PLAINS.getValue()).orElseThrow()
        );

        int bottomSectionY = chunk.getBottomY() >> 4;
        ChunkSection[] sections = chunk.getSectionArray();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            if (section == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            PalettedContainer<RegistryEntry<Biome>> container =
                    (PalettedContainer<RegistryEntry<Biome>>) section.getBiomeContainer();

            for (int quartY = 0; quartY < 4; quartY++) {
                for (int quartX = 0; quartX < 4; quartX++) {
                    for (int quartZ = 0; quartZ < 4; quartZ++) {
                        int x = chunk.getPos().getStartX() + quartX * 4 + 2;
                        int z = chunk.getPos().getStartZ() + quartZ * 4 + 2;
                        int y = ((bottomSectionY + sectionIndex) << 4) + quartY * 4 + 2;
                        MapSample sample = sampleAt(x, z, world.getSeaLevel());
                        RegistryEntry<Biome> biome = biomeFor(sample, palette);
                        if (biome != null) {
                            container.set(quartX, quartY, quartZ, biome);
                        }
                    }
                }
            }
        }
    }

    private static RegistryEntry<Biome> biomeFor(MapSample sample, BiomePalette palette) {
        return switch (sample.region()) {
            case CENTER_ISLAND -> palette.center();
            case INNER_LAKE, OUTER_WATER -> palette.ocean();
            case LONG_RIVER -> palette.river();
            case LAND -> switch (sample.sector()) {
                case 0 -> palette.snow();
                case 1 -> palette.taiga();
                case 2 -> palette.forest();
                case 3 -> palette.savanna();
                case 4 -> palette.badlands();
                default -> palette.desert();
            };
            case NORMAL -> null;
        };
    }

    private static long coordinateHash(int x, int z) {
        long value = x * 341873128712L + z * 132897987541L + 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Region {
        NORMAL,
        CENTER_ISLAND,
        INNER_LAKE,
        LAND,
        OUTER_WATER,
        LONG_RIVER
    }

    private record MapSample(Region region, int sector, int surfaceY) {
    }

    private record SurfacePalette(BlockState top, BlockState filler) {
    }

    private record BiomePalette(
            RegistryEntry<Biome> snow,
            RegistryEntry<Biome> taiga,
            RegistryEntry<Biome> forest,
            RegistryEntry<Biome> savanna,
            RegistryEntry<Biome> badlands,
            RegistryEntry<Biome> desert,
            RegistryEntry<Biome> ocean,
            RegistryEntry<Biome> river,
            RegistryEntry<Biome> center
    ) {
    }
}

package dev.windex.fusion.analysis;

import dev.windex.fusion.Config;
import dev.windex.fusion.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.Deque;

/** Incremental scanning ONLY of data already loaded in the client world. */
public final class WorldAnalyzer {
    private final Markers markers;
    private final Deque<int[]> queue = new ArrayDeque<>();
    private ScanJob job;
    private int cycleX = Integer.MIN_VALUE;
    private int cycleZ = Integer.MIN_VALUE;
    private int cycleRadius = -1;

    public WorldAnalyzer(Markers markers) { this.markers = markers; }

    public void clear() {
        queue.clear();
        job = null;
        cycleX = Integer.MIN_VALUE;
    }

    private void refill(int cx, int cz, int radius) {
        queue.clear();
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring)
                        queue.addLast(new int[]{cx+dx, cz+dz});
                }
            }
        }
        cycleX = cx; cycleZ = cz; cycleRadius = radius;
    }

    public void tick(MinecraftClient client, Config config) {
        if (client.player == null || client.world == null) { clear(); return; }
        if (!(config.on(Module.SPAWNER) || config.on(Module.SUS) || config.on(Module.CAVE)
                || config.on(Module.TERRAIN) || config.on(Module.STORAGE))) return;
        ClientWorld world = client.world;
        BlockPos player = client.player.getBlockPos();
        int cx = player.getX() >> 4, cz = player.getZ() >> 4;
        if (job != null && (job.world != world || Math.abs(job.cx-cx) > config.scanRadiusChunks+1
                || Math.abs(job.cz-cz) > config.scanRadiusChunks+1)) job = null;
        if (queue.isEmpty() && job == null || cycleRadius != config.scanRadiusChunks
                || Math.abs(cycleX - cx) > 1 || Math.abs(cycleZ - cz) > 1) {
            refill(cx, cz, config.scanRadiusChunks);
        }

        int budget = config.scanBlocksPerTick;
        while (budget > 0) {
            if (job == null) {
                int[] at = queue.pollFirst();
                if (at == null) break;
                WorldChunk chunk = world.getChunkManager().getChunk(at[0], at[1], ChunkStatus.FULL, false);
                if (chunk == null) continue;
                job = new ScanJob(world, chunk, at[0], at[1]);
            }
            int used = job.step(config, markers, budget);
            budget -= used;
            if (job.done()) {
                job.finish(config, markers);
                job = null;
            }
            if (used <= 0) break;
        }
    }

    private static final class ScanJob {
        private final ClientWorld world;
        private final WorldChunk chunk;
        private final int cx, cz;
        private final int bottom, layers;
        private int index;
        private int undergroundWeight;
        private int undergroundAnchors;
        private final int[] evidenceColumns = new int[256];
        private final int[] anchorColumns = new int[256];
        private int caveAir;
        private int surfacedArtificial;
        private int surfaceX, surfaceZ;
        private boolean blockEntitiesChecked;

        private ScanJob(ClientWorld world, WorldChunk chunk, int cx, int cz) {
            this.world = world;
            this.chunk = chunk;
            this.cx = cx;
            this.cz = cz;
            bottom = Math.max(world.getBottomY(), -60);
            layers = Math.max(1, (Math.min(96, world.getTopYInclusive()) - bottom + 3) / 3);
        }

        boolean done() { return index >= layers * 256; }

        int step(Config config, Markers markers, int allowance) {
            if (!blockEntitiesChecked) { scanBlockEntities(config, markers); blockEntitiesChecked = true; }
            int scanned = 0;
            BlockPos.Mutable pos = new BlockPos.Mutable();
            while (scanned < allowance && !done()) {
                int lx = index & 15;
                int lz = (index >>> 4) & 15;
                int y = bottom + (index >>> 8) * 3;
                index++;
                scanned++;
                int x = (cx << 4) + lx, z = (cz << 4) + lz;
                int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                if (y >= top-6) continue;
                BlockState state = chunk.getBlockState(pos.set(x,y,z));
                if (state.isAir()) {
                    if (y < top-14) caveAir++;
                    continue;
                }
                int weight = artificialWeight(state.getBlock());
                if (weight > 0) {
                    undergroundWeight += weight;
                    evidenceColumns[(lz << 4) | lx] += weight;
                    if (weight >= 3) {
                        undergroundAnchors++;
                        anchorColumns[(lz << 4) | lx]++;
                    }
                }
                // Sample only one vertical layer per column for surface evidence.
                if (y == bottom) {
                    Block surface = world.getBlockState(pos.set(x,top-1,z)).getBlock();
                    if (artificialWeight(surface) > 0) {
                        surfacedArtificial++;
                        surfaceX += x;
                        surfaceZ += z;
                    }
                }
            }
            return scanned;
        }

        private void scanBlockEntities(Config config, Markers markers) {
            for (BlockEntity entity : chunk.getBlockEntities().values()) {
                BlockPos p = entity.getPos();
                Block b = chunk.getBlockState(p).getBlock();
                String key = p.toShortString();
                if (b == Blocks.SPAWNER || b == Blocks.TRIAL_SPAWNER) {
                    if (config.on(Module.SPAWNER))
                        markers.upsert(Markers.Kind.SPAWNER, key, p.getX(),p.getY(),p.getZ(),
                                100, "Spawner block entity received", 120_000);
                } else if (isStorage(b) && config.on(Module.STORAGE)) {
                    markers.upsert(Markers.Kind.STORAGE, key, p.getX(),p.getY(),p.getZ(),
                            100, "Storage block entity received", 120_000);
                }
            }
        }

        private void finish(Config config, Markers markers) {
            String chunkKey = cx+":"+cz;
            // Choose an actual detected underground column, not a nearby chunk
            // or a midpoint that may be between unrelated areas of activity.
            if (config.on(Module.SUS) && undergroundAnchors > 0
                    && undergroundWeight >= config.susMinEvidence) {
                int best = -1;
                for (int i=0;i<evidenceColumns.length;i++) {
                    if (anchorColumns[i] > 0 && (best < 0
                            || evidenceColumns[i] > evidenceColumns[best])) best = i;
                }
                int bx = (cx << 4) + (best & 15), bz = (cz << 4) + (best >>> 4);
                Projection.Target target = Projection.surface(bx,bz,1,
                        (x,z)->world.getTopY(Heightmap.Type.WORLD_SURFACE,x,z));
                markers.upsert(Markers.Kind.SUS, chunkKey, target.x(),target.y(),target.z(),
                        Math.min(90, 30 + undergroundWeight*3),
                        "Directly above best clue column; weight "+undergroundWeight
                                +", anchors "+undergroundAnchors, 180_000);
            }
            if (config.on(Module.CAVE) && caveAir >= 140) {
                int x = cx*16+8, z = cz*16+8;
                markers.upsert(Markers.Kind.CAVE, chunkKey, x,Math.max(bottom+8,0),z,
                        Math.min(65, 15+caveAir/20),
                        "Underground air samples: "+caveAir+" (natural caves possible)", 180_000);
            }
            if (config.on(Module.TERRAIN) && surfacedArtificial >= 4) {
                int x = surfaceX / surfacedArtificial;
                int z = surfaceZ / surfacedArtificial;
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE,x,z);
                markers.upsert(Markers.Kind.TERRAIN, chunkKey, x,y,z,
                        Math.min(85, 25+surfacedArtificial*3),
                        "Surface artificial-material samples: "+surfacedArtificial, 180_000);
            }
        }
    }

    private static boolean isStorage(Block b) {
        return b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL
                || b == Blocks.HOPPER || b == Blocks.DISPENSER || b == Blocks.DROPPER
                || b == Blocks.ENDER_CHEST || b instanceof ShulkerBoxBlock;
    }

    private static int artificialWeight(Block b) {
        if (isStorage(b) || b == Blocks.CRAFTING_TABLE || b == Blocks.FURNACE
                || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER) return 5;
        if (b == Blocks.TORCH || b == Blocks.WALL_TORCH || b == Blocks.LANTERN
                || b == Blocks.SOUL_LANTERN || b == Blocks.REDSTONE_TORCH
                || b == Blocks.COBBLESTONE_STAIRS || b == Blocks.OAK_DOOR) return 3;
        if (b == Blocks.OAK_PLANKS || b == Blocks.SPRUCE_PLANKS || b == Blocks.BIRCH_PLANKS
                || b == Blocks.STONE_BRICKS || b == Blocks.GLASS || b == Blocks.GLASS_PANE
                || b == Blocks.COBBLESTONE) return 1;
        return 0;
    }
}

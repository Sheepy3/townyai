package town.sheepy.townyAI.towngrowth;

import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.scheduler.BukkitRunnable;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.model.Building;
import town.sheepy.townyAI.store.TownRegistry;
import town.sheepy.townyAI.terrain.SchematicHelper;
import town.sheepy.townyAI.terrain.TerrainHelper;

final class WallBatcher {
    private final TownyAI plugin;
    private final TownRegistry registry;
    private final World world;
    private final String townName;
    private final java.util.ArrayDeque<WallStrategy.WallTask> queue;
    private final java.util.Set<TownGrowthHelper.ChunkPos> claimed;
    private final java.util.Set<TownGrowthHelper.ChunkPos> flattenedWilderness = new java.util.HashSet<>();
    WallBatcher(TownyAI plugin,
                TownRegistry registry,
                World world,
                String townName,
                java.util.List<WallStrategy.WallTask> tasks,
                java.util.Set<TownGrowthHelper.ChunkPos> claimed) {
        this.plugin   = plugin;
        this.registry = registry;
        this.world    = world;
        this.townName = townName;
        this.queue    = new java.util.ArrayDeque<>(tasks);
        this.claimed  = claimed; // from Towny
    }

    /** Process up to perTick tasks each tick. */
    void start(int perTick, long initialDelayTicks) {
        new BukkitRunnable() {
            @Override public void run() {
                int n = Math.min(perTick, queue.size());
                for (int i = 0; i < n; i++) {
                    WallStrategy.WallTask wt = queue.poll();
                    if (wt == null) break;
                    doOne(wt);
                }
                if (queue.isEmpty()) {
                    cancel();
                    plugin.getLogger().info(townName + ": wall batch complete.");
                }
            }
        }.runTaskTimer(plugin, initialDelayTicks, 1L);
    }

    private void doOne(WallStrategy.WallTask wt) {
        try {
            Chunk chunk = world.getChunkAt(wt.chunkX, wt.chunkZ);

            // remove overwriteable buildings
            registry.getBuildingAt(townName, wt.chunkX, wt.chunkZ)
                    .ifPresent(b -> { if (b.overwriteable()) registry.removeBuilding(townName, b); });

            // Flatten wilderness neighbors
            //uses offset array instead of getAdjacent as this requires non-cardinal adjacent chunks.
            int[][] offsets = { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1},{1,-1},{-1,1},{-1,-1}};
            for (int[] off : offsets) {
                int nx = wt.chunkX + off[0];
                int nz = wt.chunkZ + off[1];
                TownGrowthHelper.ChunkPos npos = new TownGrowthHelper.ChunkPos(nx, nz);

                // Not our town's claim AND first time we touch this neighbor this batch
                if (!claimed.contains(npos) && flattenedWilderness.add(npos)) {
                    // Extra safety: ensure it's true wilderness (not another town) [TO BE IMPLEMENTED LOL]
                    //var tb = TownyAPI.getInstance().getTownBlock(world, nx, nz);
                    //boolean isWilderness = (tb == null) || !tb.hasTown();
                    //if (!isWilderness) continue;

                    Chunk neighbor = world.getChunkAt(nx, nz); // sync, main thread
                    TerrainHelper.flattenChunk(plugin, neighbor, wt.groundY);
                    // (optional) plugin.getLogger().info("Flattened wilderness at " + nx + "," + nz);
                }
            }

            // Flatten + paste schematic
            TerrainHelper.flattenChunk(plugin, chunk, wt.groundY);
            var origin = chunk.getBlock(0, wt.groundY, 0).getLocation();
            SchematicHelper.pasteSchematicFromJar(plugin, wt.schematic, origin, wt.rotDeg);

            // Register wall
            registry.addBuilding(townName, new Building("wall", 1, wt.chunkX, wt.chunkZ, false));
        } catch (Exception ex) {
            plugin.getLogger().severe("Wall batch failed at (" + wt.chunkX + "," + wt.chunkZ + "): " + ex);
        }
    }
}

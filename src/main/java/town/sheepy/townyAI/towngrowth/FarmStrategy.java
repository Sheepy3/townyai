package town.sheepy.townyAI.towngrowth;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.model.Building;
import town.sheepy.townyAI.store.TownRegistry;
import town.sheepy.townyAI.terrain.SchematicHelper;
import town.sheepy.townyAI.terrain.TerrainHelper;

import java.util.*;
import java.util.stream.Collectors;

import static town.sheepy.townyAI.towngrowth.TownGrowthHelper.chebyshevDistance;
import static town.sheepy.townyAI.towngrowth.TownGrowthHelper.getAdjacent;

public class FarmStrategy {

    private final TownyAI plugin;
    private final TownRegistry registry;

    public FarmStrategy(TownyAI plugin, TownRegistry registry) {
        this.plugin   = plugin;
        this.registry = registry;
    }

    public void placeFarm(String name, Chunk homeChunk, int groundY){
        new BukkitRunnable() {
            @Override
            public void run() {

                //List<Building> farms = registry.getBuildingsByType(name, "farm");
                FarmStrategy.pickFarmChunk(homeChunk, name, registry)
                        .ifPresent(chunk -> {
                            long adjacentFarms = registry.getBuildingsByType(name, "farm").stream()
                                    .map(b -> new TownGrowthHelper.ChunkPos(b.chunkX(), b.chunkZ()))
                                    .filter(p -> getAdjacent(p.x(), p.z()).contains(
                                            new TownGrowthHelper.ChunkPos(chunk.getX(), chunk.getZ())
                                    ))
                                    .count();
                            String schematic = adjacentFarms >= 2
                                    ? "schematics/Plains/Farm/farm_1_lvl1.schem"
                                    : "schematics/Plains/Farm/farm_2_lvl1.schem";

                            // paste
                            Location origin = chunk.getBlock(0, groundY, 0).getLocation();
                            TerrainHelper.flattenChunk(plugin, chunk, groundY);
                            int[] options = {0, 90, 180, 270};
                            int rot = options[new Random().nextInt(options.length)];
                            try {
                                SchematicHelper.pasteSchematicFromJar(plugin, schematic, origin, rot);
                                plugin.getLogger().info(String.valueOf(rot));
                            } catch (Exception e) {
                                plugin.getLogger().severe("Failed to paste schematic: " + e);
                            }

                            // register
                            registry.addBuilding(
                                    name,
                                    new Building("farm", 1,
                                            chunk.getX(), chunk.getZ(),
                                            true)
                            );
                            plugin.getLogger().info(
                                    name + " placed farm level " + 1 +
                                            " at chunk (" + chunk.getX() + "," + chunk.getZ() + ")"
                            );
                        });


            }
        }.runTask(plugin);
    }



    public static Optional<Chunk> pickFarmChunk(
            Chunk homeChunk,
            String townName,
            TownRegistry registry
    ) {
        int homeX = homeChunk.getX(), homeZ = homeChunk.getZ();

        // 1) Build occupied = only where buildings already exist
        Set<TownGrowthHelper.ChunkPos> occupied = registry.getBuildings(townName).stream()
                .map(b -> new TownGrowthHelper.ChunkPos(b.chunkX(), b.chunkZ()))
                .collect(Collectors.toSet());

        // 2) Gather all Towny-claimed chunks once
        Collection<TownBlock> townBlocks = TownyAPI.getInstance()
                .getTown(townName)
                .getTownBlocks();
        Set<TownGrowthHelper.ChunkPos> claimed = townBlocks.stream()
                .map(tb -> new TownGrowthHelper.ChunkPos(tb.getX(), tb.getZ()))
                .collect(Collectors.toSet());

        // 3) Gather farms
        List<Building> farms = registry.getBuildingsByType(townName, "farm");

        List<TownGrowthHelper.ChunkPos> candidates = new ArrayList<>();

        if (farms.isEmpty()) {
            // No farms: any claimed chunk within radius 3
            for (TownGrowthHelper.ChunkPos pos : claimed) {
                if (chebyshevDistance(homeX, homeZ, pos.x(), pos.z()) == 1
                        && !occupied.contains(pos)) {
                    candidates.add(pos);
                }
            }
        } else {
            // Farms exist: any claimed cardinal neighbor of a farm
            for (Building f : farms) {
                TownGrowthHelper.ChunkPos fpos = new TownGrowthHelper.ChunkPos(f.chunkX(), f.chunkZ());
                for (TownGrowthHelper.ChunkPos adj : getAdjacent(fpos.x(), fpos.z())) {
                    if (claimed.contains(adj)    // must be Town-claimed
                            && !occupied.contains(adj)) { // and building-free
                        candidates.add(adj);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // pick random from candidates
        TownGrowthHelper.ChunkPos pick = candidates.get(new Random().nextInt(candidates.size()));
        return Optional.of(homeChunk.getWorld().getChunkAt(pick.x(), pick.z()));
    }

}

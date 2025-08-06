package town.sheepy.townyAI.growth;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Chunk;
import town.sheepy.townyAI.model.Building;
import town.sheepy.townyAI.store.TownRegistry;

import java.util.*;
import java.util.stream.Collectors;

import static town.sheepy.townyAI.growth.TownGrowthHelper.chebyshevDistance;
import static town.sheepy.townyAI.growth.TownGrowthHelper.getAdjacent;

public class BuildingPlacement {

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

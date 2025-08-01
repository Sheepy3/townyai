package town.sheepy.townyAI.growth;

import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import town.sheepy.townyAI.store.TownRegistry;
import town.sheepy.townyAI.terrain.TerrainHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static town.sheepy.townyAI.growth.TownGrowthHelper.*;

public class Growth {


    private static final Logger log = LoggerFactory.getLogger(Growth.class);

    public static List<Chunk> selectChunksToClaim(
            Chunk homeChunk,
            String townName,
            TownRegistry registry
    ) {
        World world = homeChunk.getWorld();
        int homeX = homeChunk.getX(), homeZ = homeChunk.getZ();
        int homeY = registry.getGroundLevel(townName);
        String type = registry.getType(townName);  // "ocean" or "normal"
        Biome homeBiome = world.getBiome(homeX << 4 + 8, homeY, homeZ << 4 + 8);

        // 1) Collect claimed coords
        Set<ChunkPos> claimed = TownyAPI.getInstance()
                .getTown(townName)
                .getTownBlocks().stream()
                .map(tb -> new ChunkPos(tb.getX(), tb.getZ()))
                .collect(Collectors.toSet());

        // 2) Gather all unclaimed cardinal neighbors
        Set<ChunkPos> candidates = new HashSet<>();
        for (ChunkPos c : claimed) {
            for (ChunkPos adj : getAdjacent(c.x(), c.z())) {
                if (!claimed.contains(adj)) {
                    candidates.add(adj);
                }
            }
        }

        // 3) Convert those ChunkPos into actual Chunk objects
        List<Chunk> result = candidates.stream()
                .map(p -> world.getChunkAt(p.x(), p.z()))
                .toList();

        return result;

    }

    public static Score gradeChunk(
            Chunk candidate,
            String townName,
            TownRegistry registry
    ) {
        World world   = candidate.getWorld();

        // Home data
        int homeX     = registry.getChunkX(townName);
        int homeZ     = registry.getChunkZ(townName);



        int homeY     = registry.getGroundLevel(townName);
        String type   = registry.getType(townName);
        Biome homeBiome  = world.getBiome(homeX*16, homeY, homeZ*16);

        // Candidate data
        int x = candidate.getX(), z = candidate.getZ();
        int candY      = TerrainHelper.chunkHeightNoTree(candidate);
        //Chunk candChunk = world.getChunkAt(x,z);
        Biome candBiome   = world.getBiome(x*16, candY, z*16);

        // 1) Distance bonus
        int dist       = chebyshevDistance(homeX, homeZ, x, z);
        double distScore = 60.0 / (dist + 1);
        //JavaPlugin.getPlugin(TownyAI.class).getLogger().info(String.format(
        //        "distance: " + dist));


        // 2) Biome penalty
        double biomePenalty = 0;
        String homeBiomeId = homeBiome.getKey().value();
        String candBiomeId = candBiome.getKey().value();
        //JavaPlugin.getPlugin(TownyAI.class).getLogger().info(String.format(
        //        candBiomeId));
        boolean homeOcean = homeBiomeId.contains("ocean");
        boolean candOcean = candBiomeId.contains("ocean");
        if (homeOcean && !candOcean) biomePenalty = 25;
        if (!homeOcean && candOcean) biomePenalty = 25;

        // 3) Height penalty (prefer ground closer to homeblock)
        double heightPenalty = Math.abs(homeY - candY);

        // Total (lower = better)
        double total = -distScore + biomePenalty + heightPenalty;
        return new Score(distScore, biomePenalty, heightPenalty, total);
    }

    //score components record
    public record Score(
            double distScore,
            double biomePenalty,
            double heightBonus,
            double totalScore
    ) {}

}

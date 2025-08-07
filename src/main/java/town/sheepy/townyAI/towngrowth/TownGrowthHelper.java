package town.sheepy.townyAI.towngrowth;

import org.bukkit.Chunk;
import org.bukkit.World;
import town.sheepy.townyAI.model.Building;
import town.sheepy.townyAI.store.TownRegistry;

import java.util.*;

/**
 * Helpers for town towngrowth: adjacency, empty‑region search, and rotation offsets.
 */
public class TownGrowthHelper {

    /**
     * 1) Chebyshev distance between two chunks.
     */
    public static int chebyshevDistance(int x1, int z1, int x2, int z2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
    }

    /**
     * 2) Cardinally adjacent chunk coords around (x,z).
     */
    public static List<ChunkPos> getAdjacent(int x, int z) {
        return List.of(
                new ChunkPos(x+1, z),
                new ChunkPos(x-1, z),
                new ChunkPos(x,   z+1),
                new ChunkPos(x,   z-1)
        );
    }

    /**
     * 3) Find the north‑west‑most size×size region of *unoccupied* chunks.
     *    - homeChunk: only used to get the World
     *    - registry.getBuildings(townName) returns all existing Building entries
     *    - overrideWalls: if true, ignores any building whose level==0 (i.e. walls);
     *      if false, treats walls as “occupied” too.
     *
     * Returns the ChunkPos of the top‑left (minX,minZ) corner, or empty().
     */
    public static Optional<ChunkPos> findEmptyRegion(
            TownRegistry registry,
            String townName,
            Chunk homeChunk,
            int size,
            boolean overrideWalls
    ) {
        // pull existing buildings into a set of occupied coords
        Set<ChunkPos> occupied = new HashSet<>();
        for (Building b : registry.getBuildings(townName)) {
            if (overrideWalls || b.level() != 0) {
                occupied.add(new ChunkPos(b.chunkX(), b.chunkZ()));
            }
        }
        // also reserve the home chunk
        occupied.add(new ChunkPos(homeChunk.getX(), homeChunk.getZ()));

        int homeX = homeChunk.getX(), homeZ = homeChunk.getZ();
        int searchRadius = registry.getTargetSize(townName) + size;
        World world = homeChunk.getWorld();

        // iterate candidate top‑left corners in NW→SE order
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int cornerX = homeX + dx;
                int cornerZ = homeZ + dz;

                boolean fits = true;
                // check each of the size×size chunks
                outer:
                for (int ox = 0; ox < size; ox++) {
                    for (int oz = 0; oz < size; oz++) {
                        ChunkPos pos = new ChunkPos(cornerX + ox, cornerZ + oz);
                        if (occupied.contains(pos)) {
                            fits = false;
                            break outer;
                        }
                    }
                }
                if (fits) {
                    return Optional.of(new ChunkPos(cornerX, cornerZ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 4) Given a rotation of 0°, 90°, 180°, 270°, returns the paste‑offset
     *    (in block coordinates) from the NW‑corner of the chunk.
     *    E.g. 0° → (0,0), 90° → (16,0), 180° → (16,16), 270° → (0,16).
     */
    public static IntVector2 rotationOffset(int rotationDegrees) {
        return switch ((rotationDegrees % 360 + 360) % 360) {
            case 0   -> new IntVector2(0,  0);
            case 90  -> new IntVector2(0, 15);
            case 180 -> new IntVector2(15, 15);
            case 270 -> new IntVector2(15,  0);
            default -> throw new IllegalArgumentException("Rotation must be 0/90/180/270");
        };
    }

    /**
     * Small helper to carry a pair of ints.
     */
    public record ChunkPos(int x, int z) { }

    /**
     * Small helper to carry a pair of ints for block offsets.
     */
    public record IntVector2(int x, int z) { }
}

package town.sheepy.townyAI.towngrowth;

import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Chunk;
import org.bukkit.scheduler.BukkitRunnable;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.store.TownRegistry;
import town.sheepy.townyAI.terrain.TerrainHelper;

import java.util.*;
import java.util.stream.Collectors;

import static town.sheepy.townyAI.towngrowth.TownGrowthHelper.*;

public class WallStrategy {
    private final TownyAI plugin;
    private final TownRegistry registry;

    public WallStrategy(TownyAI plugin, TownRegistry registry) {
        this.plugin   = plugin;
        this.registry = registry;
    }

    private void placeWallsSync(String townName) {
        var blocks = TownyAPI.getInstance().getTown(townName).getTownBlocks();
        Set<ChunkPos> claimed = blocks.stream()
                .map(tb -> new ChunkPos(tb.getX(), tb.getZ()))
                .collect(Collectors.toSet());

        org.bukkit.World bworld = plugin.getServer().getWorld("world");
        int groundY = registry.getGroundLevel(townName);
        java.util.List<WallTask> tasks = new java.util.ArrayList<>();
        for (ChunkPos pos : claimed) {
            // Figure out if this chunk borders wilderness at all
            var wild = getAdjacent(pos.x(), pos.z()).stream()
                    .filter(p -> !claimed.contains(p))
                    .collect(Collectors.toList());
            int w = wild.size();
            if (w == 0) continue; // interior: skip and DO NOT touch existing buildings

            // Choose schematic & rotation (column for opposite sides)
            String schematic;
            int rotDeg;
            if (w == 2 && areOpposites(pos, wild)) {
                schematic = "schematics/Plains/Wall/wallColumn_1_lvl1.schem";   // default N–S
                rotDeg = computeColumnRotation(pos.x(), pos.z(), wild); // 0 or 90
            } else if (w == 2) {
                schematic = "schematics/Plains/Wall/wallCorner_1_lvl1.schem";   // default S+E
                rotDeg = computeWallRotation(pos.x(), pos.z(), wild, 2);
            } else if (w == 3) {
                schematic = "schematics/Plains/Wall/wallPocket_1_lvl1.schem";   // default E+S+W
                rotDeg = computeWallRotation(pos.x(), pos.z(), wild, 3);
            } else { // w == 1
                schematic = "schematics/Plains/Wall/wall_1_lvl1.schem";         // default S
                rotDeg = computeWallRotation(pos.x(), pos.z(), wild, 1);
            }
            tasks.add(new WallTask(pos.x(), pos.z(), groundY, schematic, rotDeg));
        }

        new WallBatcher(plugin,registry,bworld,townName,tasks, claimed)
                .start(/*perTick=*/3, /*initialDelayTicks=*/1L);

    }

    final class WallTask {
        final int chunkX, chunkZ, groundY, rotDeg;
        final String schematic;
        WallTask(int cx, int cz, int gy, String sch, int rot) {
            this.chunkX = cx; this.chunkZ = cz; this.groundY = gy; this.schematic = sch; this.rotDeg = rot;
        }
    }

    /**
     * Compute CCW rotation to align default-open directions with actual wilderness sides.
     *
     * @param wilderness    the positions of adjacent wilderness
     * @param count         how many wilderness sides (1,2 or 3)
     * @return CCW rotation in degrees (0,90,180,270)
     */
    private int computeWallRotation(
            int homeX, int homeZ,
            List<ChunkPos> wilderness,
            int count
    ) {
        // map each neighbor to its cardinal dir index: 0=S,1=E,2=N,3=W
        List<Integer> dirs = new ArrayList<>();
        for (ChunkPos p : wilderness) {
            int dx = p.x() - homeX, dz = p.z() - homeZ;
            if (dx==0 && dz==1)  dirs.add(0);  // south
            if (dx==1 && dz==0)  dirs.add(1);  // east
            if (dx==0 && dz==-1) dirs.add(2);  // north
            if (dx==-1&& dz==0)  dirs.add(3);  // west
        }
        Collections.sort(dirs);

        // For count=1: rotation = dirs.get(0)*90
        // For count=2 (corner): default SE => dirs [0,1] => rot=0 → map [1,2]->90, [2,3]->180, [3,0]->270
        // ...
        // For count=3 (pocket): default E+S+W => dirs [1,0,3] => rot=0 → similarly rotate
        // Here’s a simple algorithm: find the minimal rotation so that every defaultDir is in wilderness
        List<Integer> defaultDirs = switch(count) {
            case 1 -> List.of(0);
            case 2 -> List.of(0,1);
            default-> List.of(1,0,3);
        };

        for (int rot=0; rot<360; rot+=90) {
            int finalRot = rot;
            List<Integer> rd = defaultDirs.stream()
                    .map(d-> (d + finalRot /90) % 4)
                    .sorted()
                    .collect(Collectors.toList());
            if (rd.equals(dirs)) return rot;
        }
        return 0;
    }

    private boolean areOpposites(ChunkPos center, List<ChunkPos> wild) {
        if (wild.size() != 2) return false;
        int x = center.x(), z = center.z();

        boolean ns = (wild.stream().anyMatch(p -> p.x()==x && p.z()==z+1) &&   // S
                wild.stream().anyMatch(p -> p.x()==x && p.z()==z-1));     // N
        boolean ew = (wild.stream().anyMatch(p -> p.x()==x+1 && p.z()==z) &&   // E
                wild.stream().anyMatch(p -> p.x()==x-1 && p.z()==z));     // W
        return ns || ew;
    }

    private int computeColumnRotation(int cx, int cz, List<ChunkPos> wild) {
        boolean ew = (wild.stream().anyMatch(p -> p.x()==cx+1 && p.z()==cz) &&
                wild.stream().anyMatch(p -> p.x()==cx-1 && p.z()==cz));
        return ew ? 90 : 0; // 0 = N-S, 90 = E-W
    }

    private void purgeExistingWallsSync(String townName) {
        var walls = registry.getBuildingsByType(townName, "wall");
        if (walls.isEmpty()) return;

        // flatten all wall chunks first (must be main thread)
        org.bukkit.World bworld = plugin.getServer().getWorld("world");
        int groundY = registry.getGroundLevel(townName);
        for (var w : walls) {
            Chunk c = bworld.getChunkAt(w.chunkX(), w.chunkZ());
            TerrainHelper.flattenChunk(c, groundY);
        }

        // then remove all wall entries from the registry
        int removed = registry.removeBuildingsWhere(townName, b -> "wall".equals(b.type()));
        plugin.getLogger().info(townName + ": purged " + removed + " wall entries (flattened).");
    }

    public void rebuildWalls(String townName) {
        new BukkitRunnable() {
            @Override public void run() {
                purgeExistingWallsSync(townName);
                placeWallsSync(townName);  // same logic you had in buildWalls, but synchronous
            }
        }.runTask(plugin);
    }
}

package town.sheepy.townyAI.terrain;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import town.sheepy.townyAI.TownyAI;

public class TerrainHelper {

    public static final int WATER_EXPOSED_THRESHOLD = 32;
    public static final int CLIFF_THRESHOLD = 20;

    public static void flattenChunk(TownyAI plugin, Chunk chunk, int groundY) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() * 16;
        int baseZ = chunk.getZ() * 16;
        int maxY = world.getMaxHeight();

        // clear above ground blocks e.g. leaves and hills
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                for (int y = maxY - 1; y > groundY; y--) {


                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
        int exposed = countExposedWaterOrIce(chunk);
        boolean cliff = (groundY-chunkMinHeightNoTree(chunk)) >=CLIFF_THRESHOLD;
        if (exposed > WATER_EXPOSED_THRESHOLD || cliff) {
            int floor;
            if(exposed < WATER_EXPOSED_THRESHOLD) {
                floor = chunkMinHeightNoTree(chunk);
            }else{
                floor = chunkHeightNoWater(chunk);
            }

            int bottom   = Math.max(world.getMinHeight(), floor - 5);

            Location topOrigin = chunk.getBlock(0, groundY, 0).getLocation();
            try { SchematicHelper.pasteSchematicFromJar(plugin, "schematics/platformTop_1.schem", topOrigin, 0); }
            catch (Exception e) { plugin.getLogger().severe("Failed to paste platformTop_1: " + e); }

            for (int y = groundY - 2; y >= bottom; y--) {
                Location legOrigin = chunk.getBlock(0, y, 0).getLocation();
                try { SchematicHelper.pasteSchematicFromJar(plugin, "schematics/platformLegs_1.schem", legOrigin, 0,true); }
                catch (Exception e) { plugin.getLogger().severe("Failed to paste platformLegs_1 at y=" + y + ": " + e); }
            }
            return;
        }

        // --- Normal (land) fill: map surface and fill up to groundY with dirt ---
        int minSurfaceY = Integer.MAX_VALUE;
        int[][] surfaceHeights = new int[16][16];

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx, z = baseZ + dz;
                int y = world.getHighestBlockYAt(x, z);

                while (y >= world.getMinHeight() && world.getBlockAt(x, y, z).getType() == Material.WATER) {
                    y--;
                }

                surfaceHeights[dx][dz] = y;
                if (y < minSurfaceY) minSurfaceY = y;
            }
        }

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx, z = baseZ + dz;
                for (int y = minSurfaceY; y <= groundY; y++) {
                    world.getBlockAt(x, y, z).setType(Material.DIRT, false);
                }
            }
        }
    }

    //returns chunks max height
    public static int chunkHeightNoTree(Chunk chunk) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        int maxGroundY = 0;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                int y = world.getHighestBlockYAt(x, z);
                // step down until we hit non‑tree material
                while (y > 0 && isTreeBlock(world.getBlockAt(x, y, z).getType())
                        || !world.getBlockAt(x,y,z).isSolid()) {
                    y--;
                }
                if (y > maxGroundY) maxGroundY = y;
            }
        }
        return maxGroundY;
    }
    //returns chunks min height
    public static int chunkMinHeightNoTree(Chunk chunk) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        int minY = world.getMinHeight();

        int minGroundY = Integer.MAX_VALUE;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx, z = baseZ + dz;
                int y = world.getHighestBlockYAt(x, z);

                while (y > minY && (isTreeBlock(world.getBlockAt(x, y, z).getType())
                        || !world.getBlockAt(x, y, z).isSolid())) {
                    y--;
                }
                if (y < minGroundY) minGroundY = y;
            }
        }
        return (minGroundY == Integer.MAX_VALUE) ? minY : minGroundY;
    }


    //returns seafloor (chunks minimum height)
    public static int chunkHeightNoWater(Chunk chunk) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        int minY  = world.getMinHeight();

        int minGroundY = Integer.MAX_VALUE;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx, z = baseZ + dz;
                int y = world.getHighestBlockYAt(x, z);

                while (y > minY) {
                    var block = world.getBlockAt(x, y, z);
                    Material m = block.getType();
                    // Skip air, foliage, water plants, and water/ice themselves until we hit ground
                    if (m == Material.AIR || isTreeBlock(m) || isWaterPlant(m) || isWaterOrIce(m) || !block.isSolid()) {
                        y--; continue;
                    }
                    break;
                }

                if (y < minGroundY) minGroundY = y;
            }
        }

        return (minGroundY == Integer.MAX_VALUE) ? minY : minGroundY;
    }

    public static int countExposedWaterOrIce(Chunk chunk) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        int minY  = world.getMinHeight();
        int count = 0;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx, z = baseZ + dz;
                int y = world.getHighestBlockYAt(x, z);
                while (y > minY) {
                    Material m = world.getBlockAt(x, y, z).getType();
                    if (m == Material.AIR || isTreeBlock(m) || isWaterPlant(m)) { y--; continue; }
                    if (isWaterOrIce(m)) count++;
                    break;
                }
            }
        }
        return count;
    }

    private static boolean isTreeBlock(Material mat) {
        String name = mat.name();
        return name.contains("LOG")
                || name.contains("LEAVES")
                || name.contains("SAPLING")
                || name.contains("WOOD")
                || name.contains("VINE");
    }

    private static boolean isWaterOrIce(Material m) {
        return m == Material.WATER || m == Material.ICE || m == Material.PACKED_ICE || m == Material.BLUE_ICE;
    }

    // plants that sit in/above water so the column is still “exposed water”
    private static boolean isWaterPlant(Material m) {
        return m == Material.SEAGRASS || m == Material.TALL_SEAGRASS ||
                m == Material.KELP || m == Material.KELP_PLANT ||
                m == Material.LILY_PAD || m == Material.SEA_PICKLE ||
                m == Material.BUBBLE_COLUMN;
    }
}


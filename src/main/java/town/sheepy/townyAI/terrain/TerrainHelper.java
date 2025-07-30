package town.sheepy.townyAI.terrain;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
public class TerrainHelper {

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
                while (y > 0 && isTreeBlock(world.getBlockAt(x, y, z).getType()) || !world.getBlockAt(x,y,z).isSolid()) {
                    y--;
                }
                if (y > maxGroundY) {
                    maxGroundY = y;
                }
            }
        }
        return maxGroundY;
    }

    private static boolean isTreeBlock(Material mat) {
        String name = mat.name();
        return name.contains("LOG")
                || name.contains("LEAVES")
                || name.contains("SAPLING")
                || name.contains("WOOD")
                || name.contains("VINE");
    }





    public static void flattenChunk(Chunk chunk, int groundY) {
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
        //maps chunk surface and finds lowest point
        int minSurfaceY = Integer.MAX_VALUE;
        int[][] surfaceHeights = new int[16][16];
        for (int dx=0; dx<16; dx++){
            for (int dz = 0; dz <16; dz++){
                int x = baseX + dx;
                int z = baseZ + dz;

                int y = world.getHighestBlockYAt(x,z);

                while (y>=0 && world.getBlockAt(x,y,z).getType()==Material.WATER) {
                    y--;
                }

                surfaceHeights[dx][dz] = y;
                if (y<minSurfaceY){
                    minSurfaceY = y;
                }
            }
        }
        //fill in dirt
        for (int dx=0; dx < 16; dx++){
            for (int dz=0; dz<16; dz++){
                int x = baseX+dx;
                int z = baseZ+dz;
                for (int y = minSurfaceY; y<=groundY; y++)
                    world.getBlockAt(x,y,z).setType(Material.DIRT);
            }
        }


    }
}

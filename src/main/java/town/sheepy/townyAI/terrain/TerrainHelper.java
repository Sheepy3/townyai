package town.sheepy.townyAI.terrain;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
public class TerrainHelper {

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

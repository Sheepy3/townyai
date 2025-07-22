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
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                for (int y = maxY - 1; y > groundY; y--) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }


    }
}

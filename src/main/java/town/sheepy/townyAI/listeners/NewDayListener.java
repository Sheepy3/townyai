package town.sheepy.townyAI.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.store.TownRegistry;

import town.sheepy.townyAI.towngrowth.FarmStrategy;
import town.sheepy.townyAI.towngrowth.ClaimStrategy;
import town.sheepy.townyAI.towngrowth.WallStrategy;

import java.util.Random;

public class NewDayListener implements Listener {
    private final TownyAI plugin;
    private final TownRegistry registry;
    private final ClaimStrategy claimSvc;
    private final FarmStrategy farmSvc;
    private final WallStrategy wallSvc;
    public NewDayListener(TownyAI plugin){
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
        this.claimSvc = new ClaimStrategy(plugin, plugin.getRegistry());
        this.farmSvc = new FarmStrategy(plugin, plugin.getRegistry());
        this.wallSvc = new WallStrategy(plugin, plugin.getRegistry());
    }

    @EventHandler
    public void onNewDay(NewDayEvent event) {
        plugin.getLogger().info("Towny Newday triggered - Updating AI towns");
        for (Town townObj : TownyAPI.getInstance().getTowns()) {

            String name = townObj.getName();
            int chunkX = registry.getChunkX(name);
            int chunkZ = registry.getChunkZ(name);
            int groundY = registry.getGroundLevel(name);
            World world = Bukkit.getWorld("world");
            Chunk homeChunk = world.getChunkAt(chunkX, chunkZ);


            if (!registry.containsTown(name)) {
                continue;
            }
            Random rng = new Random();

            if(registry.getClaimCount(name) > 9){
                plugin.getLogger().info("building walls!");
                wallSvc.rebuildWalls(name);
            }

            if (rng.nextInt(100) < 60) {
                int claims = townObj.getTownBlocks().size();
                registry.setClaimCount(name, claims);
                int award = claims * 5;
                int prev = registry.getResources(name);
                registry.setResources(name, prev + award);
                plugin.getLogger().info(String.format(
                        "%s: %d claims → +%d resources (total %d)",
                        name, claims, award, prev + award
                ));

                int target = registry.getTargetSize(name);
                if (claims >= target) continue;

                int toClaim = Math.min(target - claims, rng.nextInt(3) + 1);
                claimSvc.claim(name, homeChunk, toClaim);

            } else {
                plugin.getLogger().info(name + " is building today instead of claiming.");
                farmSvc.placeFarm(name, homeChunk, groundY);
            }
        }
    }
}
package town.sheepy.townyAI.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.growth.Growth;
import town.sheepy.townyAI.store.TownRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class NewDayListener implements Listener {
    private final TownyAI plugin;
    private final TownRegistry registry;

    public NewDayListener(TownyAI plugin){
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
    }

    @EventHandler
    public void onNewDay(NewDayEvent event){
        plugin.getLogger().info("Towny Newday triggered - Updating AI towns");
        for (Town townObj : TownyAPI.getInstance().getTowns()){

            String name = townObj.getName();

            if (!registry.containsTown(name)) {
                continue;
            }

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

            int chunkX = registry.getChunkX(name);
            int chunkZ = registry.getChunkZ(name);
            World world = Bukkit.getWorld("world");
            Chunk homeChunk = world.getChunkAt(chunkX,chunkZ);

            Random rng = new Random();
            int canClaim = Math.min(target - claims, rng.nextInt(3) + 1);

            var candidates = Growth.selectChunksToClaim(
                    homeChunk, name, registry
            );

            if (candidates.isEmpty()) {
                plugin.getLogger().info(name + " has no adjacent candidates.");
                continue;
            }

            List<Chunk> sorted = candidates.stream()
                    .map(chunk -> new Object[]{chunk, Growth.gradeChunk(chunk, name, registry)})
                    .sorted(Comparator.comparingDouble(o -> ((Growth.Score)o[1]).totalScore()))
                    .map(o -> (Chunk)o[0])
                    .collect(Collectors.toList());

            // **Dispatch claims synchronously**
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = 0; i < canClaim && i < sorted.size(); i++) {
                        Chunk c = sorted.get(i);
                        plugin.getServer().dispatchCommand(
                                plugin.getServer().getConsoleSender(),
                                String.format("townyadmin claim %s %d %d",
                                        name, c.getX(), c.getZ())
                        );
                        registry.setClaimCount(name, registry.getClaimCount(name) + 1);
                        plugin.getLogger().info(String.format(
                                "%s auto‑claimed chunk (%d,%d) [#%d/%d]",
                                name, c.getX(), c.getZ(), i + 1, canClaim
                        ));
                    }
                }
            }.runTask(plugin);
        }
    }
}

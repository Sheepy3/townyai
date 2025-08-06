package town.sheepy.townyAI.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.growth.BuildingPlacement;
import town.sheepy.townyAI.growth.Growth;
import town.sheepy.townyAI.growth.TownGrowthHelper;
import town.sheepy.townyAI.model.Building;
import town.sheepy.townyAI.store.TownRegistry;
import town.sheepy.townyAI.terrain.SchematicHelper;
import town.sheepy.townyAI.terrain.TerrainHelper;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static town.sheepy.townyAI.growth.TownGrowthHelper.getAdjacent;

public class NewDayListener implements Listener {
    private final TownyAI plugin;
    private final TownRegistry registry;

    public NewDayListener(TownyAI plugin){
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
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


                //rng = new Random();
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
                        .sorted(Comparator.comparingDouble(o -> ((Growth.Score) o[1]).totalScore()))
                        .map(o -> (Chunk) o[0])
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
            } else {
                plugin.getLogger().info(name + " is building today instead of claiming.");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        List<Building> farms = registry.getBuildingsByType(name, "farm");
                        BuildingPlacement.pickFarmChunk(homeChunk, name, registry)
                                .ifPresent(chunk -> {
                                    long adjacentFarms = registry.getBuildingsByType(name, "farm").stream()
                                            .map(b -> new TownGrowthHelper.ChunkPos(b.chunkX(), b.chunkZ()))
                                            .filter(p -> getAdjacent(p.x(), p.z()).contains(
                                                    new TownGrowthHelper.ChunkPos(chunk.getX(), chunk.getZ())
                                            ))
                                            .count();
                                    String schematic = adjacentFarms >= 2
                                            ? "schematics/farm_1_lvl1.schem"
                                            : "schematics/farm_2_lvl1.schem";

                                    // paste
                                    Location origin = chunk.getBlock(0, groundY, 0).getLocation();
                                    TerrainHelper.flattenChunk(chunk, groundY);
                                    try {
                                        SchematicHelper.pasteSchematicFromJar(plugin, schematic, origin);
                                    } catch (Exception e) {
                                        plugin.getLogger().severe("Failed to paste schematic: " + e);
                                    }

                                    // register
                                    registry.addBuilding(
                                            name,
                                            new Building("farm", 1,
                                                    chunk.getX(), chunk.getZ(),
                                                    true)
                                    );
                                    plugin.getLogger().info(
                                            name + " placed farm level " + 1 +
                                                    " at chunk (" + chunk.getX() + "," + chunk.getZ() + ")"
                                    );
                                });


                    }
                }.runTask(plugin);
            }
        }
    }
}
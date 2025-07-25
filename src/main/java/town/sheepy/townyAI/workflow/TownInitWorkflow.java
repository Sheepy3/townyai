package town.sheepy.townyAI.workflow;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.terrain.SchematicHelper;
import town.sheepy.townyAI.terrain.TerrainHelper;

public class TownInitWorkflow implements Workflow {
    private final TownyAI plugin;      // allow access to registry & dispatch
    private final String townName;
    private final String leaderName;
    private final String code;
    private final boolean manual;
    private final int coordx, coordy, coordz;
    private int stage = 0;

    public TownInitWorkflow(TownyAI plugin,
                            String townName,
                            String leaderName,
                            String code,
                            boolean manual,
                            int coordx,
                            int coordy,
                            int coordz) {
        this.plugin     = plugin;
        this.townName   = townName;
        this.leaderName = leaderName;
        this.code       = code;
        this.manual     = manual;
        this.coordx     = coordx;
        this.coordy     = coordy;
        this.coordz     = coordz;
    }
    @Override
    public String getCode() {
        return code;
    }


    @Override
    public void start() {
        //String whisper = String.format("msg ADMINBOT createtown %s %s %s", leaderName, townName, code); //asks admin to instance town leader
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                "msg ADMINBOT createtown " + leaderName +" "+ townName+" "+code
        );
        plugin.getLogger().info("§aRequested leader spawn (code=" + code + "). Awaiting ACK #1...");
    }

    private Chunk chunk;
    private int groundY;

    @Override
    public boolean onAck(CommandSender sender) {
        stage++;
        switch (stage) {
            case 1:
                if (manual) {
                    Player leader = Bukkit.getPlayerExact(leaderName);
                    if(leader != null && leader.isOnline()){
                        leader.teleport(new Location(leader.getWorld(), coordx, coordy, coordz));
                        plugin.getLogger().info("Teleported leader manually to coordinate " +
                                coordx + ", " + coordy + ", " +coordz);

                        String ackCmd = "ack " + code;
                        plugin.getServer().dispatchCommand(
                                plugin.getServer().getConsoleSender(),
                                ackCmd
                        );
                        plugin.getLogger().info("Auto‑ACK #1 for code=" + code);
                    }else{
                        plugin.getLogger().warning("Leader bot not online!");
                    }

                }else{
                    //plugin.getLogger().info(leaderName);
                    // first ACK → request leader to RTP
                    plugin.getServer().dispatchCommand(
                            plugin.getServer().getConsoleSender(),
                            "msg " + leaderName + " rtp " + code
                    );
                    plugin.getLogger().info("§aACK #1 received. Sent RTP request.");
                }
                return false;   // not done yet
            case 2:
                // second ACK → ask leader to actually create the town in-game.
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        "msg " + leaderName + " createtown " + townName + " "+ code
                );

                return false;
            case 3:

                // third ack -> grab leader position & create town in plugin data
                Player leader = Bukkit.getPlayerExact(leaderName);
                if (leader == null || !leader.isOnline()) {
                    plugin.getLogger().info("Leader bot not online yet.");
                    return false;
                }
                chunk = leader.getLocation().getChunk();
                groundY = leader.getLocation().getBlockY()-1;
                plugin.getLogger().info(String.valueOf(groundY));
                TerrainHelper.flattenChunk(chunk, groundY);
                Location origin = leader.getLocation()
                    .getChunk()
                    .getBlock(0,groundY,0)
                    .getLocation();
                try {
                    SchematicHelper.pasteSchematicFromJar(
                            plugin,
                            "schematics/homeblock_1_lvl1.schem",
                            origin
                    );
                    plugin.getLogger().info("Vault schematic pasted.");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to paste schematic: " + e);
                }

                int baseCenterX = chunk.getX()*16 + 8;
                int baseCenterZ = chunk.getZ()*16 + 8;
                int baseCenterY = groundY+1;
                Location teleportLoc = new Location(
                        chunk.getWorld(),
                        baseCenterX,
                        baseCenterY,
                        baseCenterZ
                );
                leader.teleport(teleportLoc);
                plugin.getLogger().info("Teleported leader '" + leaderName +
                        "' to vault center at " + baseCenterX + "," + baseCenterY + "," + baseCenterZ);

                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        "msg " + leaderName + " set_t_spawn " + code
                );

                return false;
            case 4:
                //final -> register bot town in towns.yml to persist it
                boolean added = plugin.getRegistry()
                        .addTown(townName, chunk.getX(), chunk.getZ());

                plugin.getRegistry().setGroundLevel(townName, groundY);

                plugin.getLogger().info(added
                        ? "Town '" + townName + "' created at chunk (" +
                        chunk.getX() + "," + chunk.getZ() + ")."
                        : "Town already exists."
                );
                return true;


            default:
                plugin.getLogger().info("Unexpected ACK stage: " + stage);
                return true;
        }
    }
}

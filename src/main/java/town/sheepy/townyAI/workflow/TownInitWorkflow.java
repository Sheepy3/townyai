package town.sheepy.townyAI.workflow;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import town.sheepy.townyAI.TownyAI;

public class TownInitWorkflow implements Workflow {
    private final TownyAI plugin;      // allow access to registry & dispatch
    private final String townName;
    private final String leaderName;
    private final String code;
    private int stage = 0;

    public TownInitWorkflow(TownyAI plugin,
                            String townName,
                            String leaderName,
                            String code) {
        this.plugin     = plugin;
        this.townName   = townName;
        this.leaderName = leaderName;
        this.code       = code;
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

    @Override
    public boolean onAck(CommandSender sender) {
        stage++;
        switch (stage) {
            case 1:
                plugin.getLogger().info(leaderName);
                // first ACK → request leader to RTP
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        "msg " + leaderName + " rtp " + code
                );
                plugin.getLogger().info("§aACK #1 received. Sent RTP request.");
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
                    plugin.getLogger().info("§cLeader bot not online yet.");
                    return false;
                }
                var chunk = leader.getLocation().getChunk();
                boolean added = plugin.getRegistry()
                        .addTown(townName, chunk.getX(), chunk.getZ());
                plugin.getLogger().info(added
                        ? "§aTown '" + townName + "' created at chunk (" +
                        chunk.getX() + "," + chunk.getZ() + ")."
                        : "§eTown already exists."
                );
                return true;


            default:
                plugin.getLogger().info("§eUnexpected ACK stage: " + stage);
                return true;
        }
    }
}

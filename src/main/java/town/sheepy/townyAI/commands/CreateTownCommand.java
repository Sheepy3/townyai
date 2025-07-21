package town.sheepy.townyAI.commands;

import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.workflow.TownInitWorkflow;
import org.bukkit.command.*;
import java.util.UUID;

public class CreateTownCommand implements CommandExecutor {
    private final TownyAI plugin;

    public CreateTownCommand(TownyAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command cmd,
            String label,
            String[] args
    ) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /" + label + " <townName>");
            return true;
        }

        // 1) Read the town name
        String townName = args[0];

        // 2) Generate a random leaderName and code
        String leaderName = "leader" + UUID.randomUUID().toString().substring(0, 8);
        String code       = UUID.randomUUID().toString().replace("-", "");

        // 3) Create & register workflow
        TownInitWorkflow wf = new TownInitWorkflow(
                plugin,
                townName,
                leaderName,
                code
        );
        plugin.registerWorkflow(wf);
        plugin.getLogger().info("workflow started!");
        return true;
    }
}

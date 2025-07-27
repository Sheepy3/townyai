package town.sheepy.townyAI.store;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class TownRegistry {
    private final File file;
    private final YamlConfiguration cfg;

    public TownRegistry(JavaPlugin plugin){

        //ensures plugin data folder exists
        plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(),"towns.yml");

        //load or create towns.yml
        if (!file.exists()){
            //make new file
            cfg = new YamlConfiguration();
            save();

        }else{
            //load existing file
            cfg = YamlConfiguration.loadConfiguration(file);
        }
    }

    public boolean addTown(String name, int chunkX, int chunkY){
        String key = "towns." + name.toLowerCase();
        if (cfg.contains(key)) return false; //false if town with name already exists

        cfg.set(key + ".name", name);
        cfg.set(key + ".x", chunkX);
        cfg.set(key + ".z", chunkY);
        save();
        return true;
    }

    //sets the ground level of the town, generally on town initialization
    public boolean setGroundLevel(String townName, int groundY) {
        String key = "towns." + townName.toLowerCase();
        if (!cfg.contains(key)) return false;
        cfg.set(key + ".groundY", groundY);
        save();
        return true;
    }
    public int getGroundLevel(String townName) {
        return cfg.getInt("towns." + townName.toLowerCase() + ".groundY", 0);
    }

    //stores the name of the town leader on town initialization
    public boolean setLeaderName(String townName, String leaderName) {
        String key = "towns." + townName.toLowerCase();
        if (!cfg.contains(key)) return false;
        cfg.set(key + ".leader", leaderName);
        save();
        return true;
    }

    public String getLeaderName(String townName) {
        return cfg.getString("towns." + townName.toLowerCase() + ".leader", null);
    }

    //track new claim count as town grows
    public boolean setClaimCount(String townName, int claims) {
        String key = "towns." + townName.toLowerCase();
        if (!cfg.contains(key)) return false;
        cfg.set(key + ".claims", claims);
        save();
        return true;
    }

    public int getClaimCount(String townName) {
        return cfg.getInt("towns." + townName.toLowerCase() + ".claims", 0);
    }

    //track resource count as town grows
    public boolean setResources(String townName, int resources) {
        String key = "towns." + townName.toLowerCase();
        if (!cfg.contains(key)) return false;
        cfg.set(key + ".resources", resources);
        save();
        return true;
    }

    public int getResources(String townName) {
        return cfg.getInt("towns." + townName.toLowerCase() + ".resources", 0);
    }


    public void save(){
        try{
            cfg.save(file);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

}

package town.sheepy.townyAI.store;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import town.sheepy.townyAI.model.Building;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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


    public boolean setType(String townName, String type){
        String key = "towns." + townName.toLowerCase();
        if (!cfg.contains(key)) return false;
        cfg.set(key + ".type", type);
        save();
        return true;
    };

    public String getType(String townName) {
        return cfg.getString("towns." + townName.toLowerCase() + ".type", "normal");
    }

    public boolean setTargetSize(String townName, int size) {
        String key = "towns." + townName.toLowerCase();
        if (!cfg.contains(key)) return false;
        cfg.set(key + ".targetSize", size);
        save();
        return true;
    }
    public int getTargetSize(String townName) {
        return cfg.getInt("towns." + townName.toLowerCase() + ".targetSize", 0);
    }

    public boolean initBuildings(String townName) {
        String key = "towns." + townName.toLowerCase();
        if (!cfg.contains(key)) return false;
        cfg.set(key + ".buildings", List.of());
        save();
        return true;
    }

    public List<Building> getBuildings(String townName) {
        String base = "towns." + townName.toLowerCase() + ".buildings";
        List<Building> out = new ArrayList<>();
        if (!cfg.contains(base)) return out;

        for (var entry : cfg.getMapList(base)) {
            Object lvl = entry.get("level");
            Object x   = entry.get("chunkX");
            Object z   = entry.get("chunkZ");
            if (lvl instanceof Number && x instanceof Number && z instanceof Number) {
                out.add(new Building(
                        ((Number)lvl).intValue(),
                        ((Number)x).intValue(),
                        ((Number)z).intValue()
                ));
            }
        }
        return out;
    }

    public int getChunkX(String townName) {
        return cfg.getInt("towns." + townName.toLowerCase() + ".x");
    }

    public int getChunkZ(String townName) {
        return cfg.getInt("towns." + townName.toLowerCase() + ".z");
    }

    public boolean containsTown(String townName) {
        return cfg.contains("towns." + townName.toLowerCase());
    }


    public void save(){
        try{
            cfg.save(file);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

}

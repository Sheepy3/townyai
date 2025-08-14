package town.sheepy.townyAI.store;

import org.bukkit.configuration.ConfigurationSection;
import town.sheepy.townyAI.TownyAI;

import java.util.*;

public final class TownLevelService {

    public static record LevelDef(int level, int size, int maxTowns) {}

    private final TownyAI plugin;
    private final NavigableMap<Integer, LevelDef> byLevel = new TreeMap<>();
    private final Map<Integer, Integer> levelBySize = new HashMap<>();

    public TownLevelService(TownyAI plugin) {
        this.plugin = plugin;
        reload(); // load immediately
    }

    public void reload() {
        plugin.saveDefaultConfig(); // writes config.yml on first run
        plugin.reloadConfig();

        byLevel.clear();
        levelBySize.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("townlevels");
        if (root == null) throw new IllegalStateException("config.yml missing 'townlevels' section");

        for (String key : root.getKeys(false)) {
            int lvl;
            try { lvl = Integer.parseInt(key.trim()); }
            catch (NumberFormatException nfe) { throw new IllegalStateException("Non-integer level: " + key); }

            int size = root.getInt(key + ".size");
            int max  = root.getInt(key + ".max_towns");
            if (size <= 0) throw new IllegalStateException("Level " + lvl + " has invalid size");
            if (max  <  0) throw new IllegalStateException("Level " + lvl + " has invalid max_towns");

            LevelDef def = new LevelDef(lvl, size, max);
            byLevel.put(lvl, def);
            // reverse map for deriving level from targetSize later
            levelBySize.put(size, lvl);
        }

        if (byLevel.isEmpty()) throw new IllegalStateException("No townlevels configured");

        // Log what we loaded for quick verification
        StringBuilder sb = new StringBuilder("Loaded townlevels: ");
        boolean first = true;
        for (LevelDef d : byLevel.values()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("L").append(d.level()).append("(size=").append(d.size()).append(", cap=").append(d.maxTowns()).append(")");
        }
        plugin.getLogger().info(sb.toString());
    }

    public Optional<LevelDef> pickAnyEligible(Map<Integer, Integer> currentCounts) {
        java.util.List<LevelDef> eligible = new java.util.ArrayList<>();
        for (LevelDef d : byLevel.values()) {
            int used = currentCounts.getOrDefault(d.level(), 0);
            if (used < d.maxTowns()) eligible.add(d);
        }
        if (eligible.isEmpty()) return Optional.empty();
        return Optional.of(eligible.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(eligible.size())));
    }

    public Collection<LevelDef> allLevels() { return byLevel.values(); }

    public Optional<LevelDef> getLevel(int level) { return Optional.ofNullable(byLevel.get(level)); }

    public OptionalInt levelForSize(int size) {
        Integer lvl = levelBySize.get(size);
        return (lvl == null) ? OptionalInt.empty() : OptionalInt.of(lvl);
    }

    public int maxConfiguredLevel() { return byLevel.isEmpty() ? 0 : byLevel.lastKey(); }

    public OptionalInt sizeForLevel(int level) {
        LevelDef d = byLevel.get(level);
        return (d == null) ? OptionalInt.empty() : OptionalInt.of(d.size());
    }
}

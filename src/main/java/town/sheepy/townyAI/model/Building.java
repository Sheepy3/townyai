package town.sheepy.townyAI.model;


public record Building(
        String type,
        int level,
        int chunkX,
        int chunkZ,
        boolean overwriteable) { }

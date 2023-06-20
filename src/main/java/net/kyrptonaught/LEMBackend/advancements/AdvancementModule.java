package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;

import java.util.concurrent.ConcurrentHashMap;

public class AdvancementModule extends Module {
    public final ConcurrentHashMap<String, PlayerAdvancements> advancementHolderCache = new ConcurrentHashMap<>();

    public AdvancementModule() {
        super("data/advancements");
    }

    public PlayerAdvancements getAdvancementsFor(String player) {
        if (advancementHolderCache.containsKey(player))
            return advancementHolderCache.get(player);

        PlayerAdvancements playerAdvancements = loadFromFile(player);
        if (playerAdvancements == null)
            playerAdvancements = new PlayerAdvancements();

        advancementHolderCache.put(player, playerAdvancements);
        return playerAdvancements;
    }

    public void registerAdvancementsFor(String player, PlayerAdvancements playerAdvancements) {
        advancementHolderCache.put(player, playerAdvancements);
    }

    @Override
    public void load(Gson gson) {
        createDirectories();
    }

    @Override
    public void save(Gson gson) {
        createDirectories();
        advancementHolderCache.keySet().forEach(this::saveAdvancements);
    }

    public void saveAdvancements(String player) {
        createDirectories();
        writeFile(player + ".json", LEMBackend.gson.toJson(advancementHolderCache.get(player)));
    }

    public void unloadAdvancements(String player) {
        saveAdvancements(player);
        advancementHolderCache.remove(player);
    }

    private PlayerAdvancements loadFromFile(String player) {
        JsonObject obj = load(player, LEMBackend.config.gson);
        if (obj == null) return null;

        return AdvancementSerializer.deserialize(obj);
    }

    private JsonObject load(String player, Gson gson) {
        return readFileJson(gson, player + ".json", JsonObject.class);
    }
}

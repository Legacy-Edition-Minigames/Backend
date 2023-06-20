package net.kyrptonaught.LEMBackend.userConfig;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserConfigModule extends Module {

    public final ConcurrentHashMap<String, PlayerConfigs> playerCache = new ConcurrentHashMap<>();

    public UserConfigModule() {
        super("data/userConfigs");
    }

    public PlayerConfigs getPlayerConfig(String player) {
        if (!playerCache.containsKey(player))
            load(player);

        return playerCache.get(player);
    }

    public void unloadPlayer(String player) {
        if (playerCache.containsKey(player)) {
            save(player);
            playerCache.remove(player);
        }
    }

    public void setValue(String player, String key, String value) {
        getPlayerConfig(player).setValue(key, value);
        save(player);
    }


    public ConcurrentHashMap<String, String> getValues(String player) {
        return getPlayerConfig(player).getValues();
    }

    public void removeValue(String player, String key) {
        getPlayerConfig(player).removeValue(key);
        save(player);
    }

    public void saveToPreset(String player, String presetID, JsonObject keys) {
        getPlayerConfig(player).saveToPreset(presetID, keys);
        save(player);
    }

    public void loadFromPreset(String player, String presetID, JsonArray keys) {
        getPlayerConfig(player).loadFromPreset(presetID, keys);
        save(player);
    }

    public void load(String player) {
        JsonObject userConfigObj = readFileJson(LEMBackend.gson, player + ".json", JsonObject.class);
        playerCache.put(player, PlayerConfigs.load(userConfigObj));
    }

    public void save(String player) {
        writeFile(player + ".json", LEMBackend.gson.toJson(playerCache.get(player)));
    }

    @Override
    public void load(Gson gson) {
        createDirectories();
    }

    @Override
    public void save(Gson gson) {
        createDirectories();

        playerCache.keySet().forEach(this::save);
    }
}
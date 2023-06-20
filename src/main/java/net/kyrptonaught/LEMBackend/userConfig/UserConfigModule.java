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

    public final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> userConfigs = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, String>>> userConfigPresets = new ConcurrentHashMap<>();

    public UserConfigModule() {
        super("data/userConfigs");
    }

    public void setValue(String player, String key, String value) {
        setValueInternal(player, key, value);
        saveUserConfigs();
    }

    public void setValueInternal(String player, String key, String value) {
        if (!userConfigs.containsKey(player)) userConfigs.put(player, new ConcurrentHashMap<>());

        userConfigs.get(player).put(key, value);
    }

    public ConcurrentHashMap<String, String> getValues(String player) {
        return userConfigs.get(player);
    }

    public void removeValue(String player, String key) {
        if (userConfigs.containsKey(player)) {
            userConfigs.get(player).remove(key);

            if (userConfigs.get(player).size() == 0)
                userConfigs.remove(player);
            saveUserConfigs();
        }
    }

    public void saveToPreset(String player, String presetID, JsonObject keys) {
        if (!userConfigs.containsKey(player)) userConfigs.put(player, new ConcurrentHashMap<>());

        if (userConfigPresets.containsKey(player))
            userConfigPresets.get(player).remove(presetID);

        for (Map.Entry<String, JsonElement> element : keys.entrySet()) {
            setPresetValueInternal(player, presetID, element.getKey(), element.getValue().getAsString());
        }
        saveConfigPresets();
    }

    public void loadFromPreset(String player, String presetID, JsonArray keys) {
        if (!userConfigPresets.containsKey(player) || !userConfigPresets.get(player).containsKey(presetID)) return;

        if (!userConfigs.containsKey(player)) userConfigs.put(player, new ConcurrentHashMap<>());

        for (JsonElement element : keys) {
            String key = element.getAsString();
            userConfigs.get(player).put(key, userConfigPresets.get(player).get(presetID).get(key));
        }
        saveUserConfigs();
    }

    private void setPresetValueInternal(String player, String presetID, String key, String value) {
        if (!userConfigPresets.containsKey(player)) userConfigPresets.put(player, new ConcurrentHashMap<>());
        var playerPresets = userConfigPresets.get(player);
        if (!playerPresets.containsKey(presetID)) playerPresets.put(presetID, new ConcurrentHashMap<>());

        playerPresets.get(presetID).put(key, value);
    }


    @Override
    public void load(Gson gson) {
        createDirectories();

        JsonObject userConfigObj = readFileJson(gson, "userConfigs.json", JsonObject.class);
        for (Map.Entry<String, JsonElement> entry : userConfigObj.entrySet())
            for (Map.Entry<String, JsonElement> innerEntry : entry.getValue().getAsJsonObject().entrySet())
                setValueInternal(entry.getKey(), innerEntry.getKey(), innerEntry.getValue().getAsString());

        JsonObject userGroupObj = readFileJson(gson, "userConfigsPresets.json", JsonObject.class);
        for (Map.Entry<String, JsonElement> entry : userGroupObj.entrySet())
            for (Map.Entry<String, JsonElement> innerEntry : entry.getValue().getAsJsonObject().entrySet())
                for (Map.Entry<String, JsonElement> innerEntry2 : innerEntry.getValue().getAsJsonObject().entrySet())
                    setPresetValueInternal(entry.getKey(), innerEntry.getKey(), innerEntry2.getKey(), innerEntry2.getValue().getAsString());
    }

    @Override
    public void save(Gson gson) {
        createDirectories();
        saveUserConfigs();
        saveConfigPresets();
    }

    private void saveUserConfigs() {
        writeFile("userConfigs.json", LEMBackend.gson.toJson(userConfigs));
    }

    private void saveConfigPresets() {
        writeFile("userConfigsPresets.json", LEMBackend.gson.toJson(userConfigPresets));
    }
}
package net.kyrptonaught.LEMBackend.userConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerConfigs {

    public final ConcurrentHashMap<String, String> configs = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> presets = new ConcurrentHashMap<>();


    public void setValue(String key, String value) {
        configs.put(key, value);
    }

    public ConcurrentHashMap<String, String> getValues() {
        return configs;
    }

    public void removeValue(String key) {
        configs.remove(key);
    }

    public void saveToPreset(String presetID, JsonObject keys) {
        presets.remove(presetID);

        for (Map.Entry<String, JsonElement> element : keys.entrySet()) {
            setPresetValueInternal(presetID, element.getKey(), element.getValue().getAsString());
        }
    }

    private void setPresetValueInternal(String presetID, String key, String value) {
        if (!presets.containsKey(presetID)) presets.put(presetID, new ConcurrentHashMap<>());

        presets.get(presetID).put(key, value);
    }

    public void loadFromPreset(String presetID, JsonArray keys) {
        if (!presets.containsKey(presetID)) return;

        for (JsonElement element : keys) {
            String key = element.getAsString();
            configs.put(key, presets.get(presetID).get(key));
        }
    }

    public static PlayerConfigs load(JsonObject jsonObject) {
        PlayerConfigs playerConfigs = new PlayerConfigs();
        if (jsonObject == null) return playerConfigs;

        JsonObject configs = jsonObject.getAsJsonObject("configs");
        for (Map.Entry<String, JsonElement> innerEntry : configs.entrySet())
            playerConfigs.setValue(innerEntry.getKey(), innerEntry.getValue().getAsString());

        JsonObject presets = jsonObject.getAsJsonObject("presets");
        for (Map.Entry<String, JsonElement> innerEntry : presets.entrySet())
            for (Map.Entry<String, JsonElement> innerEntry2 : innerEntry.getValue().getAsJsonObject().entrySet())
                playerConfigs.setPresetValueInternal(innerEntry.getKey(), innerEntry2.getKey(), innerEntry2.getValue().getAsString());

        return playerConfigs;
    }
}

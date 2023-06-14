package net.kyrptonaught.LEMBackend.userConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.LEMBackend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserConfigHolder {

    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> userConfigs = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, String>>> userConfigPresets = new ConcurrentHashMap<>();

    protected static final Path dir = Paths.get("data/userConfigs");

    public static void setValue(String player, String key, String value) {
        setValueInternal(player, key, value);
        save();
    }

    public static void setValueInternal(String player, String key, String value) {
        if (!userConfigs.containsKey(player)) userConfigs.put(player, new ConcurrentHashMap<>());

        userConfigs.get(player).put(key, value);
    }

    public static ConcurrentHashMap<String, String> getValues(String player) {
        return userConfigs.get(player);
    }

    public static void removeValue(String player, String key) {
        if (userConfigs.containsKey(player)) {
            userConfigs.get(player).remove(key);

            if (userConfigs.get(player).size() == 0)
                userConfigs.remove(player);
            save();
        }
    }

    public static void saveToPreset(String player, String presetID, JsonArray keys) {
        if (!userConfigs.containsKey(player)) userConfigs.put(player, new ConcurrentHashMap<>());

        for (JsonElement element : keys) {
            String key = element.getAsString();
            setPresetValue(player, presetID, key, userConfigs.get(player).get(key));
        }
        save();
    }

    public static void loadFromPreset(String player, String presetID, JsonArray keys) {
        if (!userConfigPresets.containsKey(player) || !userConfigPresets.get(player).containsKey(presetID)) return;

        if (!userConfigs.containsKey(player)) userConfigs.put(player, new ConcurrentHashMap<>());

        for (JsonElement element : keys) {
            String key = element.getAsString();
            userConfigs.get(player).put(key, userConfigPresets.get(player).get(presetID).get(key));
        }
        save();
    }

    public static void setPresetValue(String player, String presetID, String key, String value) {
        if (!userConfigPresets.containsKey(player)) userConfigPresets.put(player, new ConcurrentHashMap<>());
        var playerPresets = userConfigPresets.get(player);
        if (!playerPresets.containsKey(presetID)) playerPresets.put(presetID, new ConcurrentHashMap<>());

        playerPresets.get(presetID).put(key, value);
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        createDirectories();
        Path saveFile = dir.resolve("userConfigs.json");
        if (Files.exists(saveFile) && Files.isReadable(saveFile)) {
            try (InputStream in = Files.newInputStream(saveFile, StandardOpenOption.READ); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject obj = LEMBackend.config.gson.fromJson(reader, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    for (Map.Entry<String, JsonElement> innerEntry : entry.getValue().getAsJsonObject().entrySet()) {
                        setValueInternal(entry.getKey(), innerEntry.getKey(), innerEntry.getValue().getAsString());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        saveFile = dir.resolve("userConfigsPresets.json");
        if (Files.exists(saveFile) && Files.isReadable(saveFile)) {
            try (InputStream in = Files.newInputStream(saveFile, StandardOpenOption.READ); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject obj = LEMBackend.config.gson.fromJson(reader, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : obj.entrySet())
                    for (Map.Entry<String, JsonElement> innerEntry : entry.getValue().getAsJsonObject().entrySet())
                        for (Map.Entry<String, JsonElement> innerEntry2 : innerEntry.getValue().getAsJsonObject().entrySet())
                            setPresetValue(entry.getKey(), innerEntry.getKey(), innerEntry2.getKey(), innerEntry2.getValue().getAsString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        createDirectories();
        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(dir.resolve("userConfigs.json")), StandardCharsets.UTF_8)) {
            String json = LEMBackend.config.gson.toJson(userConfigs);
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(dir.resolve("userConfigsPresets.json")), StandardCharsets.UTF_8)) {
            String json = LEMBackend.config.gson.toJson(userConfigPresets);
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package net.kyrptonaught.LEMBackend.userConfig;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserConfigHolder {

    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> userConfigs = new ConcurrentHashMap<>();
    protected static final Path dir = Paths.get("data/userConfigs");

    public static void setValue(String player, String key, String value) {
        if (!userConfigs.containsKey(player)) userConfigs.put(player, new ConcurrentHashMap<>());

        userConfigs.get(player).put(key, value);
        save();
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
                        setValue(entry.getKey(), innerEntry.getKey(), innerEntry.getValue().getAsString());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        createDirectories();
        Path saveFile = dir.resolve("userConfigs.json");
        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(saveFile), StandardCharsets.UTF_8)) {
            String json = LEMBackend.config.gson.toJson(userConfigs);
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package net.kyrptonaught.LEMBackend.keyValueStorage;

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
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueHolder {

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> storage = new ConcurrentHashMap<>();
    private static final Path dir = Paths.get("data/kvs");


    public static String getValue(String id, String key) {
        return getIdStorage(id).get(key);
    }


    public static JsonObject getValueAsJson(String id, String key) {
        return result(getValue(id, key));
    }

    public static void setValue(String id, String key, String value) {
        getIdStorage(id).put(key, value);
    }

    public static void resetValue(String id, String key) {
        getIdStorage(id).remove(key);
    }

    public static ConcurrentHashMap<String, String> getIdStorage(String id) {
        if (!storage.containsKey(id))
            storage.put(id, new ConcurrentHashMap<>());

        return storage.get(id);
    }


    private static JsonObject result(String value) {
        JsonObject result = new JsonObject();
        result.addProperty("value", value);
        return result;
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
        Path saveFile = dir.resolve("kvs.json");
        if (Files.exists(saveFile) && Files.isReadable(saveFile)) {
            try (InputStream in = Files.newInputStream(saveFile, StandardOpenOption.READ); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject obj = LEMBackend.gson.fromJson(reader, JsonObject.class);
                obj.entrySet().forEach(outer -> {
                    String id = outer.getKey();
                    JsonObject inner = outer.getValue().getAsJsonObject();
                    inner.entrySet().forEach(innerElement -> {
                        setValue(id, innerElement.getKey(), innerElement.getValue().getAsString());
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        createDirectories();
        Path saveFile = dir.resolve("kvs.json");
        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(saveFile), StandardCharsets.UTF_8)) {
            String json = LEMBackend.gson.toJson(storage);
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

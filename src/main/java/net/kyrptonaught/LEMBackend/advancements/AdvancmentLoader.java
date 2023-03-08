package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.Gson;
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

public class AdvancmentLoader {
    public static final ConcurrentHashMap<String, AdvancementHolder> advancementHolderCache = new ConcurrentHashMap<>();
    private static final Path dir = Paths.get("data/advancements");

    public static AdvancementHolder getAdvancementsFor(String player) {
        if (advancementHolderCache.containsKey(player))
            return advancementHolderCache.get(player);

        AdvancementHolder advancementHolder = loadFromFile(player);
        if (advancementHolder == null)
            advancementHolder = new AdvancementHolder();

        advancementHolderCache.put(player, advancementHolder);
        return advancementHolder;
    }

    public static void createDirectories() {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void registerAdvancementsFor(String player, AdvancementHolder advancementHolder) {
        advancementHolderCache.put(player, advancementHolder);
    }

    public static void saveAdvancements(String player) {
        save(player, advancementHolderCache.get(player), LEMBackend.config.gson);
    }

    public static void saveAdvancements() {
        advancementHolderCache.forEach((s, advancementHolder) -> {
            save(s, advancementHolder, LEMBackend.config.gson);
        });
    }

    public static void unloadAdvancements(String player) {
        saveAdvancements(player);
        advancementHolderCache.remove(player);
    }

    private static AdvancementHolder loadFromFile(String player) {
        JsonObject obj = load(player, LEMBackend.config.gson);
        if (obj == null) return null;

        return AdvancementHolder.Serializer.deserialize(obj);
    }

    private static JsonObject load(String player, Gson gson) {
        Path saveFile = dir.resolve(player + ".json");
        if (Files.exists(saveFile) && Files.isReadable(saveFile)) {
            try (InputStream in = Files.newInputStream(saveFile, StandardOpenOption.READ); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, JsonObject.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void save(String player, AdvancementHolder advancementHolder, Gson gson) {
        createDirectories();
        Path saveFile = dir.resolve(player + ".json");
        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(saveFile), StandardCharsets.UTF_8)) {
            String json = gson.toJson(advancementHolder);
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

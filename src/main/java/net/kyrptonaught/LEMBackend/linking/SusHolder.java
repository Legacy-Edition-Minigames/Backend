package net.kyrptonaught.LEMBackend.linking;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.kyrptonaught.LEMBackend.LEMBackend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

public class SusHolder {
    private static final Path dir = LinkHolder.dir;

    private static final HashSet<String> sussies = new HashSet<>();

    public static void addSus(String mcUUID) {
        sussies.add(mcUUID);
    }

    public static boolean isSus(String mcUUID) {
        return sussies.contains(mcUUID);
    }

    public static void removeSus(String mcUUID) {
        sussies.remove(mcUUID);
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
        Path saveFile = dir.resolve("sus.json");
        if (Files.exists(saveFile) && Files.isReadable(saveFile)) {
            try (InputStream in = Files.newInputStream(saveFile, StandardOpenOption.READ); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonArray obj = LEMBackend.config.gson.fromJson(reader, JsonArray.class);
                for (JsonElement item : obj) {
                    sussies.add(item.getAsString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        createDirectories();
        Path saveFile = dir.resolve("sus.json");
        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(saveFile), StandardCharsets.UTF_8)) {
            String json = LEMBackend.config.gson.toJson(sussies);
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

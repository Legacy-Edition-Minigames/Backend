package net.kyrptonaught.LEMBackend.whitelistSync;

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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class WhitelistHolder {

    public static final List<WhitelistEntry> whitelist = Collections.synchronizedList(new ArrayList<>());
    protected static final Path dir = Paths.get("data/whitelist");

    public static void add(String uuid, String name) {
        for (WhitelistEntry entry : whitelist) {
            if (entry.name.equals(name) || entry.uuid.equals(uuid)) {
                return;
            }
        }
        whitelist.add(new WhitelistEntry(uuid, name));
        save();
    }

    public static void remove(String uuid, String name) {
        for (int i = whitelist.size() - 1; i >= 0; i--) {
            WhitelistEntry entry = whitelist.get(i);
            if (entry.name.equals(name) || entry.uuid.equals(uuid)) {
                whitelist.remove(entry);
                save();
            }
        }
    }

    public static WhitelistEntry[] getList() {
        return whitelist.toArray(WhitelistEntry[]::new);
    }

    public static void clear() {
        whitelist.clear();
        save();
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
        Path saveFile = dir.resolve("whitelist.json");
        if (Files.exists(saveFile) && Files.isReadable(saveFile)) {
            try (InputStream in = Files.newInputStream(saveFile, StandardOpenOption.READ); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonArray obj = LEMBackend.config.gson.fromJson(reader, JsonArray.class);
                for (JsonElement entry : obj) {
                    add(entry.getAsJsonObject().get("uuid").getAsString(), entry.getAsJsonObject().get("name").getAsString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        createDirectories();
        Path saveFile = dir.resolve("whitelist.json");
        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(saveFile), StandardCharsets.UTF_8)) {
            String json = LEMBackend.config.gson.toJson(whitelist.toArray(WhitelistEntry[]::new));
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class WhitelistEntry {
        public String uuid;
        public String name;

        public WhitelistEntry(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
package net.kyrptonaught.LEMBackend.linking;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LinkHolder {

    private static final List<Link> links = new ArrayList<>();
    private static final ConcurrentHashMap<String, Link> mcToLinks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Link> discordToLinks = new ConcurrentHashMap<>();

    private static final Path dir = Paths.get("data/links");

    public static void addLink(String mcUUID, String discordID) {
        addLink(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE), mcUUID, discordID);
    }

    public static void addLink(String dateLinked, String mcUUID, String discordID) {
        if (mcToLinks.containsKey(mcUUID) || discordToLinks.containsKey(discordID))
            return;
        Link link = new Link(dateLinked, mcUUID, discordID);
        links.add(link);
        mcToLinks.put(mcUUID, link);
        discordToLinks.put(discordID, link);
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
        Path saveFile = dir.resolve("links.json");
        if (Files.exists(saveFile) && Files.isReadable(saveFile)) {
            try (InputStream in = Files.newInputStream(saveFile, StandardOpenOption.READ); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonArray obj = LEMBackend.config.gson.fromJson(reader, JsonArray.class);
                for (JsonElement item : obj) {
                    JsonObject link = item.getAsJsonObject();
                    addLink(link.get("dateLinked").getAsString(), link.get("mcUUID").getAsString(), link.get("discordID").getAsString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        createDirectories();
        Path saveFile = dir.resolve("links.json");
        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(saveFile), StandardCharsets.UTF_8)) {
            String json = LEMBackend.config.gson.toJson(links);
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Link {
        private final String dateLinked;
        private final String mcUUID;
        private final String discordID;

        public Link(String dateLinked, String mcUUID, String discordID) {
            this.dateLinked = dateLinked;
            this.mcUUID = mcUUID;
            this.discordID = discordID;
        }
    }
}

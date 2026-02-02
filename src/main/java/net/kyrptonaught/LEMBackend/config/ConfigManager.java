package net.kyrptonaught.LEMBackend.config;

import com.google.gson.*;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class ConfigManager {
    public static Path dir;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setStrictness(Strictness.LENIENT)
            .registerTypeAdapter(Identifier.class, new IdentifierSerializer())
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .create();


    public static Gson getGSON() {
        return GSON;
    }

    public static <T> void save(String file, T config) {
        Path saveFile = dir.resolve(file + ".json");
        try (OutputStream os = Files.newOutputStream(saveFile); OutputStreamWriter out = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            out.write(GSON.toJson(config));
        } catch (Exception e) {
            System.out.println("Unable to save: " + saveFile);
            e.printStackTrace();
        }
    }

    public static <T> T load(Path saveFile, T config) {
        if (!Files.exists(saveFile) || !Files.isReadable(saveFile)) {
            System.out.println("Unable to load: " + saveFile);
            return config;
        }

        try (InputStream in = Files.newInputStream(saveFile, StandardOpenOption.READ);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return (T) GSON.fromJson(reader, config.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Unable to load: " + saveFile);
        return config;
    }

    public static <T> T load(String file, T config) {
        T newConfig = load(dir.resolve(file + ".json"), config);
        save(file, newConfig);
        return newConfig;
    }

    private static class IdentifierSerializer implements JsonDeserializer<Identifier>, JsonSerializer<Identifier> {
        public Identifier deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return Identifier.of(jsonElement.getAsString());
        }

        public JsonElement serialize(Identifier identifier, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(identifier.toString());
        }
    }

    private static class InstantSerializer implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Instant.parse(json.getAsString());
        }

        public JsonElement serialize(Instant instant, Type type, JsonSerializationContext JsonDeserializationContext) {
            return new JsonPrimitive(instant.toString());
        }
    }
}

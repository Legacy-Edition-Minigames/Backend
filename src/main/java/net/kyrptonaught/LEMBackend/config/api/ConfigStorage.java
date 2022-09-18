package net.kyrptonaught.LEMBackend.config.api;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConfigStorage {
    private final Path saveFile;
    public AbstractConfigFile config;
    private final AbstractConfigFile defaultConfig;

    public ConfigStorage(Path fileName, AbstractConfigFile defaultConfig) {
        this.saveFile = fileName;
        this.defaultConfig = defaultConfig;
    }

    public void save(String MOD_ID, Gson JANKSON) {
        try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(saveFile), StandardCharsets.UTF_8)) {
            String json = JANKSON.toJson(config);
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(MOD_ID + " Failed to save " + saveFile.getFileName().toString());
        }
    }

    public AbstractConfigFile load(String MOD_ID, Gson JANKSON) {
        if (!Files.exists(saveFile) || !Files.isReadable(saveFile)) {
            System.out.println(MOD_ID + " Config not found! Creating one.");
            config = defaultConfig;
            return config;
        }
        boolean failed = false;
        try {
            InputStreamReader reader = new InputStreamReader(Files.newInputStream(saveFile, StandardOpenOption.READ), StandardCharsets.UTF_8);
            config = JANKSON.fromJson(reader, defaultConfig.getClass());
        } catch (Exception e) {
            failed = true;
        }
        if (failed || (config == null)) {
            System.out.println(MOD_ID + " Failed to load config! Overwriting with default config.");
            config = defaultConfig;
        }
        return config;
    }
}
package net.kyrptonaught.LEMBackend.config.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyrptonaught.LEMBackend.advancements.AdvancementHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class ConfigManager {
    public Gson gson;
    protected final HashMap<String, ConfigStorage> configs = new HashMap<>();
    protected Path dir;
    protected String MOD_ID;

    private ConfigManager(String mod_id) {
        this.MOD_ID = mod_id;
        gson = new GsonBuilder()
                .registerTypeAdapter(AdvancementHolder.class, new AdvancementHolder.Serializer())
                .serializeNulls()
                .setPrettyPrinting()
                .setLenient()
                .create();
    }

    public void setDir(Path dir) {
        this.dir = dir;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AbstractConfigFile getConfig(String name) {
        if (!name.endsWith(".json")) name = name + ".json";
        return configs.get(name).config;
    }

    public void registerFile(String name, AbstractConfigFile defaultConfig) {
        if (!name.endsWith(".json")) name = name + ".json";
        configs.put(name, new ConfigStorage(dir.resolve(name), defaultConfig));
    }

    public void save() {
        configs.values().forEach(configStorage -> configStorage.save(MOD_ID, gson));
    }

    public void load(boolean saves) {
        configs.values().forEach(configStorage -> configStorage.load(MOD_ID, gson));
        if (saves) save();
    }


    public static class SingleConfigManager extends ConfigManager {
        public SingleConfigManager(String mod_id, AbstractConfigFile defaultConfig) {
            super(mod_id);
            registerFile(mod_id + "config", defaultConfig);
        }

        public AbstractConfigFile getConfig() {
            return getConfig(MOD_ID + "config");
        }
    }

    public static class MultiConfigManager extends ConfigManager {
        public MultiConfigManager(String mod_id) {
            super(mod_id);
            dir = Path.of(dir + "/" + MOD_ID);
        }

        public void load(String config) {
            if (!config.endsWith(".json")) config = config + ".json";
            this.configs.get(config).load(MOD_ID, gson);
            save(config);
        }

        public void save(String config) {
            if (!config.endsWith(".json")) config = config + ".json";
            this.configs.get(config).save(MOD_ID, gson);
        }
    }
}
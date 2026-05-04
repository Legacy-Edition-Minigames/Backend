package net.kyrptonaught.LEMBackend.resourcer;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.IO;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.config.ConfigManager;
import net.minecraft.SharedConstants;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringDecomposer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class Resourcer {
    private static final String ID = "heirloom";
    private static final String EN_US = "en_us";

    public static String getLang(String version, String rpVersion, String lang) {
        Path finalPath = FabricLoader.getInstance().getGameDir().resolve(ID).resolve("cache").resolve("lang").resolve(version + "-" + rpVersion).resolve(lang + ".json");
        if (!FileHelper.exists(finalPath)) {
            Path basePath = cacheDownload("https://assets.mcasset.cloud/" + version + "/assets/minecraft/lang/" + lang + ".json", Path.of("lang").resolve(version).resolve(lang + ".json"));
            String base = FileHelper.readFile(basePath);

            String url = "https://github.com/kyrptonaught/Minigame-Resources/releases/download/" + rpVersion + "/vanilla.zip";
            Path zipPath = cacheDownload(url, Path.of("resource packs").resolve("lem.base_vanilla(" + rpVersion + ").zip"));
            Path langPath = FabricLoader.getInstance().getGameDir().resolve(ID).resolve("cache").resolve("lang").resolve(rpVersion).resolve(lang + ".json");

            if (!FileHelper.exists(langPath)) {
                FileHelper.writeFile(langPath, FileHelper.readFileFromZip(zipPath, "assets/minecraft/lang/" + lang + ".json"));
                System.out.println("Downloaded: " + langPath);
            }
            String lem = FileHelper.readFile(langPath);

            FileHelper.writeFile(finalPath, base.substring(0, base.length() - 2) + ",\n" + lem.substring(1));
        }

        return FileHelper.readFile(finalPath);
    }

    private static Path cacheDownload(String url, Path file) {
        file = FabricLoader.getInstance().getGameDir().resolve(ID).resolve("cache").resolve(file);
        if (!FileHelper.exists(file)) {
            FileHelper.download(url, file);
            System.out.println("Downloaded: " + url);
        }
        return file;
    }

    public static void injectTranslations() {
        HashMap<String, String> builder = new HashMap<>();

        JsonObject obj = LEMBackend.gson.fromJson(IO.getAlt("https://api.github.com/repos/kyrptonaught/Minigame-Resources/releases"), JsonArray.class).get(0).getAsJsonObject();

        String version = SharedConstants.getCurrentVersion().name();
        String rpVersion = obj.get("tag_name").getAsString();
        loadFromJson(ConfigManager.getGSON().fromJson(Resourcer.getLang(version, rpVersion, EN_US), JsonObject.class), builder::put);

        final ImmutableMap<String, String> map = ImmutableMap.copyOf(builder);
        Language.inject(new Language() {

            @Override
            public String getOrDefault(String key, String fallback) {
                return map.getOrDefault(key, fallback);
            }

            @Override
            public boolean has(String key) {
                return map.containsKey(key);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return false;
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return visitor -> text.visit((style, string) -> StringDecomposer.iterateFormatted(string, style, visitor) ? Optional.empty() : FormattedText.STOP_ITERATION, Style.EMPTY).isPresent();
            }
        });
    }

    private static void loadFromJson(JsonObject entries, final BiConsumer<String, String> output) {
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            String text = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]").matcher(GsonHelper.convertToString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
            output.accept(entry.getKey(), text);
        }
    }
}
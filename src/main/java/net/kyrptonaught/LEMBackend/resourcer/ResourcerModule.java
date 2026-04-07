package net.kyrptonaught.LEMBackend.resourcer;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.IO;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;
import net.minecraft.SharedConstants;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.StringDecomposer;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

public class ResourcerModule extends Module {
    private static final String ID = "heirloom";

    @Override
    protected void load() {
        generateLobbyMusicPack(SharedConstants.getCurrentVersion().name());
    }

    public String getLang(String version, String rpVersion, String lang) {
        Path finalPath = FabricLoader.getInstance().getGameDir().resolve("cache").resolve("lang").resolve(version + "-" + rpVersion).resolve(lang + ".json");
        if (!FileHelper.exists(finalPath)) {
            Path basePath = cacheDownload("https://assets.mcasset.cloud/" + version + "/assets/minecraft/lang/" + lang + ".json", Path.of("lang").resolve(version).resolve(lang + ".json"));
            String base = FileHelper.readFile(basePath);

            String url = "https://github.com/kyrptonaught/Minigame-Resources/releases/download/" + rpVersion + "/vanilla.zip";
            Path zipPath = cacheDownload(url, Path.of("resource packs").resolve("lem.base_vanilla(" + rpVersion + ").zip"));
            Path langPath = FabricLoader.getInstance().getGameDir().resolve("cache").resolve("lang").resolve(rpVersion).resolve(lang + ".json");

            if (!FileHelper.exists(langPath)) {
                FileHelper.writeFile(langPath, FileHelper.readFileFromZip(zipPath, "assets/minecraft/lang/" + lang + ".json"));
                System.out.println("Downloaded: " + langPath);
            }
            String lem = FileHelper.readFile(langPath);

            FileHelper.writeFile(finalPath, base.substring(0, base.length() - 2) + ",\n" + lem.substring(1));
        }

        return FileHelper.readFile(finalPath);
    }

    public void checkResourcePackHash(JsonArray packs) {
        for (JsonElement pack : packs) {
            String hash = hashResourcePack(pack.getAsJsonObject());
            pack.getAsJsonObject().addProperty("hash", hash);
        }
    }

    public String hashResourcePack(JsonObject pack) {
        try {
            String str = pack.get("addon_id").getAsString().replace('/', '_').replace(':', '_');
            Path file = cacheDownload(pack.get("url").getAsString(), Path.of("resource packs").resolve(str + "(" + pack.get("version").getAsString() + ").zip"));
            return HttpUtil.hashFile(file, Hashing.sha1()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void generateLobbyMusicPack(String version) {
        if (FileHelper.exists(FabricLoader.getInstance().getGameDir().resolve("cache").resolve("music packs").resolve(version))) return;

        System.out.println("Generating Music Packs for " + version);
        String baseURL = "https://assets.mcasset.cloud/" + version + "/assets/minecraft";

        String soundResponse = IO.getAlt(baseURL + "/sounds.json");

        if (soundResponse != null && !soundResponse.isEmpty()) {
            JsonObject obj = LEMBackend.gson.fromJson(soundResponse, JsonObject.class);
            if (obj != null && obj.has("music.game")) {
                generateMusicAddon(obj, version, baseURL);
                generateMusicMuter(obj, version);
                generateHeirloomMusic(obj, version);
            }
        }
    }

    public void injectTranslations() {
        JsonObject obj = LEMBackend.gson.fromJson(IO.getAlt("https://api.github.com/repos/kyrptonaught/Minigame-Resources/releases"), JsonArray.class).get(0).getAsJsonObject();

        String version = SharedConstants.getCurrentVersion().name();
        String rpVersion = obj.get("tag_name").getAsString();

        HashMap<String, String> builder = new HashMap<>();

        try (InputStream in = URI.create(IO.getApiUrl("resourcer/lang/" + version + "/" + rpVersion + "/en_us")).toURL().openStream()) {
            Language.loadFromJson(in, builder::put);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    private static void generateMusicMuter(JsonObject obj, String version) {
        Path file = FabricLoader.getInstance().getGameDir().resolve("cache").resolve("music packs").resolve(version).resolve("muter").resolve("sounds.json");

        JsonObject out = new JsonObject();
        for (String key : obj.keySet()) {
            if (key.startsWith("music.")) {
                JsonObject value = new JsonObject();
                value.addProperty("replace", true);
                value.add("sounds", new JsonArray());
                out.add(key, value);
            }
        }

        FileHelper.createDir(file.getParent());
        FileHelper.writeFile(file, LEMBackend.gson.toJson(out));
    }

    private static void generateHeirloomMusic(JsonObject obj, String version) {
        Path file = FabricLoader.getInstance().getGameDir().resolve("cache").resolve("music packs").resolve(version).resolve("heirloom").resolve("sounds.json");

        JsonObject out = new JsonObject();
        for (String key : obj.keySet()) {
            if (key.startsWith("music.")) {
                JsonArray sounds = obj.getAsJsonObject(key).getAsJsonArray("sounds");
                for (int i = 0; i < sounds.size(); i++) {
                    JsonObject sound = sounds.get(i).getAsJsonObject();
                    sound.remove("weight");
                    JsonObject value = new JsonObject();
                    JsonArray arr = new JsonArray(1);
                    arr.add(sound);
                    value.add("sounds", arr);
                    out.add(sound.get("name").getAsString().replaceAll("/", "."), value);
                }

            }
        }
        out.remove("music.game");

        FileHelper.createDir(file.getParent());
        FileHelper.writeFile(file, LEMBackend.gson.toJson(out));
    }

    private static void generateMusicAddon(JsonObject obj, String version, String baseURL) {
        Path file = FabricLoader.getInstance().getGameDir().resolve("cache").resolve("music packs").resolve(version).resolve("addon").resolve("lobby.json");

        JsonObject object = new JsonObject();
        object.addProperty("addon_type", "music_pack");
        object.addProperty("addon_id", "lem.base:lobby");
        object.addProperty("play_order", "INIT_RANDOM");
        object.addProperty("delay", "120-300");


        LinkedHashMap<Identifier, Integer> songs = new LinkedHashMap<>();
        songs.put(Identifier.fromNamespaceAndPath("heirloom", "delay"), 20);

        JsonArray arr = obj.getAsJsonObject("music.game").getAsJsonArray("sounds");
        for (JsonElement element : arr) {
            int duration = hashMusicSong(element.getAsJsonObject().get("name").getAsString(), version, baseURL);
            songs.put(Identifier.fromNamespaceAndPath(ID, element.getAsJsonObject().get("name").getAsString().replaceAll("/", ".")), duration);
        }

        object.add("songs", LEMBackend.gson.toJsonTree(songs));

        FileHelper.createDir(file.getParent());
        FileHelper.writeFile(file, LEMBackend.gson.toJson(obj));
    }

    private static int hashMusicSong(String asset, String version, String baseURL) {
        Path file = Path.of("music packs").resolve(version).resolve("ogg").resolve(asset.replaceAll("/", "_") + ".ogg");
        file = cacheDownload(baseURL + "/sounds/" + asset + ".ogg", file);
        return Mth.ceil(calculateDuration(file) / 1000) + 2;
    }

    private static double calculateDuration(Path ogg) {
        long rate = -1;
        long length = -1;

        try {
            byte[] t = Files.readAllBytes(ogg);
            int size = t.length;

            for (int i = size - 1 - 8 - 2 - 4; i >= 0 && length < 0; i--) {
                if (t[i] == (byte) 'O' && t[i + 1] == (byte) 'g' && t[i + 2] == (byte) 'g' && t[i + 3] == (byte) 'S') {
                    byte[] byteArray = new byte[]{t[i + 6], t[i + 7], t[i + 8], t[i + 9], t[i + 10], t[i + 11], t[i + 12], t[i + 13]};
                    ByteBuffer bb = ByteBuffer.wrap(byteArray);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    length = bb.getInt(0);
                }
            }
            for (int i = 0; i < size - 8 - 2 - 4 && rate < 0; i++) {
                if (t[i] == (byte) 'v' && t[i + 1] == (byte) 'o' && t[i + 2] == (byte) 'r' && t[i + 3] == (byte) 'b' && t[i + 4] == (byte) 'i' && t[i + 5] == (byte) 's') {
                    byte[] byteArray = new byte[]{t[i + 11], t[i + 12], t[i + 13], t[i + 14]};
                    ByteBuffer bb = ByteBuffer.wrap(byteArray);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    rate = bb.getInt(0);
                }

            }

            return (double) (length * 1000) / (double) rate;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    private static Path cacheDownload(String url, Path file) {
        file = FabricLoader.getInstance().getGameDir().resolve("cache").resolve(file);
        if (!FileHelper.exists(file)) {
            FileHelper.download(url, file);
            System.out.println("Downloaded: " + url);
        }
        return file;
    }
}
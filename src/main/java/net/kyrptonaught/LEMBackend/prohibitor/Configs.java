package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.LEMBackend;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Configs {
    public static final Set<String> chatBlacklist = new HashSet<>();
    public static final Set<String> chatWhitelist = new HashSet<>();

    public static void loadChatFilter() {
        JsonObject obj = LEMBackend.ProhibitorModule.module.readFileJson(LEMBackend.gson, "chatfilter.json", JsonObject.class);

        if (obj != null && !obj.isEmpty() && obj.has("blacklist")) {
            for (JsonElement s : obj.getAsJsonArray("blacklist")) {
                chatBlacklist.add(s.getAsString());
            }
        }
        if (obj != null && !obj.isEmpty() && obj.has("whitelist")) {
            for (JsonElement s : obj.getAsJsonArray("whitelist")) {
                chatWhitelist.add(s.getAsString());
            }
        }
    }

    public static void saveChatFilter() {
        LEMBackend.ProhibitorModule.module.writeFileJson(LEMBackend.gson, "chatfilter.json", ChatFilter.json());
    }


    public static final HashMap<String, String> punishmentPresets = new HashMap<>();

    public static void loadPunishmentPresets() {
        JsonObject obj = LEMBackend.ProhibitorModule.module.readFileJson(LEMBackend.gson, "punishmentPresets.json", JsonObject.class);

        if (obj != null && !obj.isEmpty()) {
            for (String s : obj.keySet()) {
                punishmentPresets.put(s, obj.get(s).getAsString());
            }
        }
    }

    public static void savePunishmentPresets() {
        LEMBackend.ProhibitorModule.module.writeFileJson(LEMBackend.gson, "punishmentPresets.json", punishmentPresets);
    }
}
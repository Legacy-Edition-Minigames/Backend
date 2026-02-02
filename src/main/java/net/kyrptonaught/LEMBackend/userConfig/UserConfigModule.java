package net.kyrptonaught.LEMBackend.userConfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;
import net.kyrptonaught.LEMBackend.discordBridge.PatreonTier;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;

public class UserConfigModule extends Module {

    public UserConfigModule() {
        super("userConfigs");
    }

    public JsonObject loadPlayer(String player) {
        JsonObject obj = readFileJson(LEMBackend.gson, player + ".json", JsonObject.class);
        if (obj == null)
            obj = new JsonObject();

        integrations(player, obj);
        return obj;
    }

    public void syncPlayer(String player, String json) {
        writeFile(player + ".json", json);
    }

    @Override
    public void load(Gson gson) {
        createDirectories();
    }

    public void integrations(String player, JsonObject obj) {
        PlayerEntry playerEntry = LEMBackend.ProhibitorModule.module.loadPlayer(player);

        obj.addProperty("lem.base:muted", !LEMBackend.ProhibitorModule.module.canPlayerChat(player));
        obj.addProperty("lem.base:suspicious", playerEntry.sussyStatus != null);
        if (playerEntry.discordLink == null) {
            obj.addProperty("lem.base:discord_linked", false);
            obj.addProperty("lem.base:patreon_tier", PatreonTier.NONE.toString());
        } else {
            obj.addProperty("lem.base:discord_linked", true);
            obj.addProperty("lem.base:patreon_tier", LEMBackend.BridgeModule.module.getPatreonTier(playerEntry.discordLink.discordID()).toString());
        }
    }
}
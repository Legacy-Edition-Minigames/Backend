package net.kyrptonaught.LEMBackend.legacy.userConfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;

public class UserConfigModule extends Module {

    public UserConfigModule() {
        super("legacyUserConfigs");
    }

    public JsonObject loadPlayer(String player) {
        JsonObject obj = readFileJson(LEMBackend.gson, player + ".json", JsonObject.class);
        if (obj == null) obj = new JsonObject();

        JsonObject integrations = new JsonObject();
        LEMBackend.UserConfigModule.module.integrations(player, integrations);
        if (!obj.has("configs")) obj.add("configs", new JsonObject());
        for (String s : integrations.keySet()) {
            obj.getAsJsonObject("configs").add(s, integrations.get(s));
        }

        return obj;
    }

    public void syncPlayer(String player, String json) {
        writeFile(player + ".json", json);
    }

    @Override
    public void load(Gson gson) {
        createDirectories();
    }
}
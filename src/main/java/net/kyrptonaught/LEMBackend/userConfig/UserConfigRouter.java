package net.kyrptonaught.LEMBackend.userConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.ModuleRouter;

import java.util.concurrent.ConcurrentHashMap;

public class UserConfigRouter extends ModuleRouter<UserConfigModule> {

    @Override
    public void addRoutes() {
        route(HTTP.GET, "/v0/{secret}/getUserConfig/{uuid}", this::getUserConfig);
        route(HTTP.POST, "/v0/{secret}/syncUserConfig/{uuid}/{key}/{value}", this::syncUserConfig);
        route(HTTP.POST, "/v0/{secret}/removeUserConfig/{uuid}/{key}", this::removeUserConfig);
        route(HTTP.POST, "/v0/{secret}/userConfigSaveToPreset/{uuid}/{preset}", this::saveToPreset);
        route(HTTP.POST, "/v0/{secret}/userConfigLoadFromPreset/{uuid}/{preset}", this::loadFromPreset);
    }

    public void getUserConfig(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        ConcurrentHashMap<String, String> dataStorage = module.getValues(uuid);
        ctx.json(dataStorage);
    }

    public void syncUserConfig(Context ctx) {
        String key = ctx.pathParam("key");
        String uuid = ctx.pathParam("uuid");
        String value = ctx.pathParam("value");

        module.setValue(uuid, key, value);
        ctx.result("success");
    }

    public void removeUserConfig(Context ctx) {
        String key = ctx.pathParam("key");
        String uuid = ctx.pathParam("uuid");

        module.removeValue(uuid, key);
        ctx.result("success");
    }

    public void saveToPreset(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        String presetID = ctx.pathParam("preset");

        JsonObject keys = ctx.bodyAsClass(JsonObject.class);
        module.saveToPreset(uuid, presetID, keys);
        ctx.result("success");
    }

    public void loadFromPreset(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        String presetID = ctx.pathParam("preset");

        JsonArray keys = ctx.bodyAsClass(JsonArray.class);
        module.loadFromPreset(uuid, presetID, keys);
        ctx.result("success");
    }
}

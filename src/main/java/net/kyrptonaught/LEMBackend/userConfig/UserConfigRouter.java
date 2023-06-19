package net.kyrptonaught.LEMBackend.userConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.LEMBackend;

import java.util.concurrent.ConcurrentHashMap;

public class UserConfigRouter {

    public static void getUserConfig(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        ConcurrentHashMap<String, String> dataStorage = UserConfigHolder.getValues(uuid);
        if (dataStorage != null && LEMBackend.secretsMatch(secret)) {
            ctx.json(dataStorage);
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void syncUserConfig(Context ctx) {
        String secret = ctx.pathParam("secret");
        String key = ctx.pathParam("key");
        String uuid = ctx.pathParam("uuid");
        String value = ctx.pathParam("value");

        if (LEMBackend.secretsMatch(secret)) {
            UserConfigHolder.setValue(uuid, key, value);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void removeUserConfig(Context ctx) {
        String secret = ctx.pathParam("secret");
        String key = ctx.pathParam("key");
        String uuid = ctx.pathParam("uuid");

        if (LEMBackend.secretsMatch(secret)) {
            UserConfigHolder.removeValue(uuid, key);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void saveToPreset(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");
        String presetID = ctx.pathParam("preset");

        JsonObject keys = ctx.bodyAsClass(JsonObject.class);

        if (LEMBackend.secretsMatch(secret)) {
            UserConfigHolder.saveToPreset(uuid, presetID, keys);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void loadFromPreset(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");
        String presetID = ctx.pathParam("preset");

        JsonArray keys = ctx.bodyAsClass(JsonArray.class);

        if (LEMBackend.secretsMatch(secret)) {
            UserConfigHolder.loadFromPreset(uuid, presetID, keys);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }
}

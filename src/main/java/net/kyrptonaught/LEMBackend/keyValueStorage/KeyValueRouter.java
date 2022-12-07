package net.kyrptonaught.LEMBackend.keyValueStorage;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.LEMBackend;

public class KeyValueRouter {

    public static void getValue(Context ctx) {
        String secret = ctx.pathParam("secret");
        String id = ctx.pathParam("id");
        String key = ctx.pathParam("key");

        if (LEMBackend.secretsMatch(secret)) {
            ctx.json(KeyValueHolder.getValueAsJson(id, key));
            return;
        }
        ctx.result("failed");
    }

    public static void setValue(Context ctx) {
        String secret = ctx.pathParam("secret");
        String id = ctx.pathParam("id");
        String key = ctx.pathParam("key");
        String value = ctx.pathParam("value");

        if (LEMBackend.secretsMatch(secret)) {
            KeyValueHolder.setValue(id, key, value);
            ctx.json(result(true));
            return;
        }
        ctx.json(result(false));
    }

    public static void resetValue(Context ctx) {
        String secret = ctx.pathParam("secret");
        String id = ctx.pathParam("id");
        String key = ctx.pathParam("key");

        if (LEMBackend.secretsMatch(secret)) {
            KeyValueHolder.resetValue(id, key);
            ctx.json(result(true));
            return;
        }
        ctx.json(result(false));
    }

    public static JsonObject result(boolean success) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", success);
        return obj;
    }
}

package net.kyrptonaught.LEMBackend.keyValueStorage;

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
            ctx.result("success");
            return;
        }
        ctx.result("failed");
    }
    public static void resetValue(Context ctx) {
        String secret = ctx.pathParam("secret");
        String id = ctx.pathParam("id");
        String key = ctx.pathParam("key");

        if (LEMBackend.secretsMatch(secret)) {
            KeyValueHolder.resetValue(id, key);
            ctx.result("success");
            return;
        }
        ctx.result("failed");
    }
}

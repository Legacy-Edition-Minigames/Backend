package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.LEMBackend;

import java.util.Map;

public class AdvancementRouter {

    public static void getAdvancements(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        AdvancementHolder advancementHolder = AdvancmentLoader.getAdvancementsFor(uuid);
        if (advancementHolder != null && LEMBackend.secretsMatch(secret)) {
            ctx.json(advancementHolder);
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void addAdvancement(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null && LEMBackend.secretsMatch(secret)) {
            AdvancementHolder advancementHolder = AdvancmentLoader.getAdvancementsFor(uuid);
            object.remove("DataVersion").getAsInt();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                advancementHolder.addAdvancementCriteria(entry.getKey(), entry.getValue().getAsJsonObject(), false);
            }

            AdvancmentLoader.saveAdvancements(uuid);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void removeAdvancement(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null && LEMBackend.secretsMatch(secret)) {
            AdvancementHolder other = AdvancmentLoader.getAdvancementsFor(uuid);

            String advancement = object.get("advancement").getAsString();
            String criteria = object.get("criteria").getAsString();
            other.removeAdvancement(advancement, criteria);

            AdvancmentLoader.saveAdvancements(uuid);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void overwriteAdvancements(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null && LEMBackend.secretsMatch(secret)) {
            AdvancementHolder advancementHolder = AdvancementHolder.Serializer.deserialize(object);
            AdvancmentLoader.registerAdvancementsFor(uuid, advancementHolder);
            AdvancmentLoader.saveAdvancements(uuid);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void unloadPlayer(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        if (LEMBackend.secretsMatch(secret)) {
            AdvancmentLoader.unloadAdvancements(uuid);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }
}

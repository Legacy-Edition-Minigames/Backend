package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.LEMBackend;

public class AdvancementRouter {

    public static void getAdvancements(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        AdvancementHolder advancementHolder = AdvancmentLoader.getAdvancementsFor(uuid);
        if (advancementHolder != null && secretsMatch(secret))
            ctx.json(advancementHolder);
        else
            ctx.result("failed");
    }

    public static void addAdvancement(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null && secretsMatch(secret)) {
            AdvancementHolder advancementHolder = AdvancementHolder.Serializer.deserialize(object);
            AdvancementHolder other = AdvancmentLoader.getAdvancementsFor(uuid);
            if (other != null) {
                other.mergeAdvancements(advancementHolder);
            } else {
                AdvancmentLoader.registerAdvancementsFor(uuid, advancementHolder);
            }
            AdvancmentLoader.saveAdvancements(uuid);
        }
    }

    public static void removeAdvancement(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null && secretsMatch(secret)) {
            AdvancementHolder other = AdvancmentLoader.getAdvancementsFor(uuid);
            JsonArray array = object.getAsJsonArray("advancements");
            array.forEach(jsonElement -> other.removeAdvancement(jsonElement.getAsString()));
            AdvancmentLoader.saveAdvancements(uuid);
        }
    }

    public static void overwriteAdvancements(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null && secretsMatch(secret)) {
            AdvancementHolder advancementHolder = AdvancementHolder.Serializer.deserialize(object);
            AdvancmentLoader.registerAdvancementsFor(uuid, advancementHolder);
            AdvancmentLoader.saveAdvancements(uuid);
        }
    }

    public static void unloadPlayer(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");

        if (secretsMatch(secret))
            AdvancmentLoader.unloadAdvancements(uuid);
    }

    public static boolean secretsMatch(String secret) {
        return LEMBackend.getConfig().secretKey.equals(secret);
    }
}

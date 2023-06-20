package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.ModuleRouter;

import java.util.Map;

public class AdvancementRouter extends ModuleRouter<AdvancementModule> {

    @Override
    public void addRoutes() {
        route(HTTP.GET, "/v0/{secret}/getAdvancements/{uuid}", this::getAdvancements);
        route(HTTP.GET, "/v0/{secret}/unloadPlayer/{uuid}", this::unloadPlayer);
        route(HTTP.POST, "/v0/{secret}/addAdvancements/{uuid}", this::addAdvancement);
        route(HTTP.POST, "/v0/{secret}/overwriteAdvancements/{uuid}", this::overwriteAdvancements);
        route(HTTP.POST, "/v0/{secret}/removeAdvancements/{uuid}", this::removeAdvancement);
    }

    public void getAdvancements(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        PlayerAdvancements playerAdvancements = module.getAdvancementsFor(uuid);
        if (playerAdvancements != null) {
            ctx.json(playerAdvancements);
            return;
        }

        ctx.status(500).result("failed");
    }

    public void addAdvancement(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null) {
            PlayerAdvancements playerAdvancements = module.getAdvancementsFor(uuid);
            object.remove("DataVersion").getAsInt();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                playerAdvancements.addAdvancementCriteria(entry.getKey(), entry.getValue().getAsJsonObject(), false);
            }

            module.saveAdvancements(uuid);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public void removeAdvancement(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null) {
            PlayerAdvancements other = module.getAdvancementsFor(uuid);

            String advancement = object.get("advancement").getAsString();
            String criteria = object.get("criteria").getAsString();
            other.removeAdvancement(advancement, criteria);

            module.saveAdvancements(uuid);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public void overwriteAdvancements(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        JsonObject object = ctx.bodyAsClass(JsonObject.class);

        if (object != null) {
            PlayerAdvancements playerAdvancements = AdvancementSerializer.deserialize(object);
            module.registerAdvancementsFor(uuid, playerAdvancements);
            module.saveAdvancements(uuid);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public void unloadPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        module.unloadAdvancements(uuid);
        ctx.result("success");
    }
}

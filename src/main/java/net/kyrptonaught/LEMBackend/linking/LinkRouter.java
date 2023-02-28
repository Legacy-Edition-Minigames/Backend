package net.kyrptonaught.LEMBackend.linking;

import com.google.gson.JsonObject;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.LEMBackend;

public class LinkRouter {

    public static void linkPlayer(Context ctx) {
        String secret = ctx.pathParam("secret");
        String mcUUID = ctx.pathParam("mcuuid");
        String discordID = ctx.pathParam("discordid");

        if (LEMBackend.secretsMatch(secret)) {
            LinkHolder.addLink(mcUUID, discordID);
            ctx.json(result(true));
            return;
        }
        ctx.result("failed");
    }

    public static JsonObject result(boolean success) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", success);
        return obj;
    }
}

package net.kyrptonaught.LEMBackend.linking;

import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.LEMBackend;

public class LinkRouter {

    public static void linkPlayer(Context ctx) {
        String secret = ctx.pathParam("secret");
        String linkID = ctx.pathParam("linkid");
        String discordID = ctx.pathParam("discordid");

        if (LEMBackend.secretsMatch(secret)) {
            String mcUUID = LinkHolder.finishLink(linkID, discordID);
            if (mcUUID != null) {
                ctx.result(mcUUID);
                return;
            }
        }
        ctx.status(500).result("failed");
    }

    public static void startLink(Context ctx) {
        String secret = ctx.pathParam("secret");
        String linkID = ctx.pathParam("linkid");
        String mcUUID = ctx.pathParam("mcuuid");

        if (LEMBackend.secretsMatch(secret)) {
            LinkHolder.startLink(linkID, mcUUID);
            ctx.result("success");
            return;
        }
        ctx.status(500).result("failed");
    }
}

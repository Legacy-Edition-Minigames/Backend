package net.kyrptonaught.LEMBackend.whitelistSync;

import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.LEMBackend;

public class WhitelistRouter {

    public static void addWhitelist(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");
        String name = ctx.pathParam("mcname");

        if (LEMBackend.secretsMatch(secret)) {
            WhitelistHolder.add(uuid, name);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void removeWhitelist(Context ctx) {
        String secret = ctx.pathParam("secret");
        String uuid = ctx.pathParam("uuid");
        String name = ctx.pathParam("mcname");

        if (LEMBackend.secretsMatch(secret)) {
            WhitelistHolder.remove(uuid, name);
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void getWhitelist(Context ctx) {
        String secret = ctx.pathParam("secret");

        if (LEMBackend.secretsMatch(secret)) {
            ctx.json(WhitelistHolder.getList());
            return;
        }

        ctx.status(500).result("failed");
    }

    public static void clearWhitelist(Context ctx) {
        String secret = ctx.pathParam("secret");

        if (LEMBackend.secretsMatch(secret)) {
            WhitelistHolder.clear();
            ctx.result("success");
            return;
        }

        ctx.status(500).result("failed");
    }
}
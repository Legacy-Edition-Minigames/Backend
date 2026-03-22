package net.kyrptonaught.LEMBackend.resourcer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.ModuleRouter;

public class ResourcerRouter extends ModuleRouter<ResourcerModule> {

    @Override
    public ResourcerModule createModule() {
        return new ResourcerModule();
    }

    @Override
    public void addRoutes(RoutesConfig routes) {
        route(routes, HTTP.GET, "/v1/{secret}/resourcer/lang/{version}/{rpversion}/{lang}", this::getLang);
        route(routes, HTTP.POST, "/v1/{secret}/resourcer/rp/hashs", this::hashRPs);
        route(routes, HTTP.POST, "/v1/{secret}/resourcer/rp/hash", this::hashRP);
    }

    private void hashRPs(Context ctx) {
        JsonArray packs = ctx.bodyAsClass(JsonArray.class);
        module.checkResourcePackHash(packs);
        ctx.result(packs.toString());
    }

    private void hashRP(Context ctx) {
        JsonObject packs = ctx.bodyAsClass(JsonObject.class);
        ctx.result(module.hashResourcePack(packs));
    }

    private void getLang(Context ctx) {
        String version = ctx.pathParam("version");
        String rpVersion = ctx.pathParam("rpversion");
        String lang = ctx.pathParam("lang");

        ctx.result(module.getLang(version, rpVersion, lang));
    }
}
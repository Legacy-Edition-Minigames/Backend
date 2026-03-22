package net.kyrptonaught.LEMBackend.userConfig;

import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.ModuleRouter;

public class UserConfigRouter extends ModuleRouter<UserConfigModule> {

    @Override
    public UserConfigModule createModule() {
        return new UserConfigModule();
    }

    @Override
    public void addRoutes(RoutesConfig routes) {
        route(routes, HTTP.GET, "/v1/{secret}/getUserConfig/{uuid}", this::getUserConfig);
        route(routes, HTTP.POST, "/v1/{secret}/syncUserConfig/{uuid}", this::syncUserConfig);
    }

    public void getUserConfig(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        ctx.json(module.loadPlayer(uuid));
    }

    public void syncUserConfig(Context ctx) {
        String uuid = ctx.pathParam("uuid");

        module.syncPlayer(uuid, ctx.body());
        ctx.result("success");
    }
}

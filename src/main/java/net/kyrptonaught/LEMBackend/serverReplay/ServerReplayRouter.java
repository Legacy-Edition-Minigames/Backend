package net.kyrptonaught.LEMBackend.serverReplay;

import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import net.kyrptonaught.LEMBackend.ModuleRouter;

public class ServerReplayRouter extends ModuleRouter<ServerReplayModule> {

    @Override
    public ServerReplayModule createModule() {
        return new ServerReplayModule();
    }

    @Override
    public void addRoutes(RoutesConfig routes) {
        route(routes, HTTP.POST, "/v1/{secret}/serverreplay/up", this::upload);
    }

    public void upload(Context ctx) {
        String json = ctx.formParam("json");
        UploadedFile file = ctx.uploadedFile("file");

        module.upload(json, file);
        ctx.result("success");
    }
}

package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonObject;
import io.javalin.config.RoutesConfig;
import net.kyrptonaught.LEMBackend.ModuleRouter;

import java.time.Duration;

public class BridgeRouter extends ModuleRouter<BridgeModule> {

    @Override
    public BridgeModule createModule() {
        return new BridgeModule();
    }

    @Override
    public void addRoutes(RoutesConfig routes) {
        routes.ws("/v1/{secret}/bridge/{bridge}", ws -> {
            ws.onConnect(ctx -> {
                if (!checkSecret(ctx)) {
                    ctx.closeSession();
                    return;
                }
                ctx.session.setIdleTimeout(Duration.ofDays(1));
                module.registerServer(ctx.pathParam("bridge"), ctx);
            });
            ws.onMessage(ctx -> BridgeIn.onMessage(ctx.pathParam("bridge"), ctx.messageAsClass(JsonObject.class)));
            ws.onClose(ctx -> module.removeServer(ctx));
            //ws.onError(ctx -> ctx.error()ctx.error().printStackTrace());
        });
    }
}

package net.kyrptonaught.LEMBackend;

import io.javalin.http.Context;
import io.javalin.websocket.WsContext;

import java.util.function.Consumer;

public abstract class ModuleRouter<T extends Module> {

    protected enum HTTP {POST, GET}

    public T module;


    public ModuleRouter() {
        setModule(createModule());
    }

    protected abstract T createModule();

    private void setModule(T module) {
        this.module = module;
    }

    public void addRoutes() {

    }

    public void route(HTTP method, String route, Consumer<Context> execute) {
        if (method == HTTP.POST) LEMBackend.app.post(route, context -> checkSecret(context, execute));
        else if (method == HTTP.GET) LEMBackend.app.get(route, context -> checkSecret(context, execute));
    }

    public void checkSecret(Context ctx, Consumer<Context> execute) {
        if (LEMBackend.secretsMatch(ctx.pathParam("secret"))) {
            execute.accept(ctx);
            return;
        }
        ctx.status(500).result("failed");
    }

    public boolean checkSecret(WsContext ctx) {
        return LEMBackend.secretsMatch(ctx.pathParam("secret"));
    }
}

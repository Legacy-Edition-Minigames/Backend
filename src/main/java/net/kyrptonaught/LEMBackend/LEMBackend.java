package net.kyrptonaught.LEMBackend;

import com.google.gson.Gson;
import io.javalin.Javalin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.LEMBackend.config.ConfigManager;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeRouter;
import net.kyrptonaught.LEMBackend.keyValueStorage.KeyValueRouter;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorRouter;
import net.kyrptonaught.LEMBackend.resourcer.ResourcerRouter;
import net.kyrptonaught.LEMBackend.userConfig.UserConfigRouter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

public class LEMBackend implements ModInitializer {
    private static ServerConfig config;
    public static Gson gson = ConfigManager.getGSON();
    public static Javalin app;
    public static MinecraftServer minecraftServer;
    public static ProhibitorRouter ProhibitorModule;
    public static UserConfigRouter UserConfigModule;
    public static KeyValueRouter KeyValueModule;
    public static BridgeRouter BridgeModule;
    public static ResourcerRouter ResourcerModule;

    public static void start() {
        config = ConfigManager.load(getBaseConfigPath().resolve("LEMBackendConfig.json"), new ServerConfig());

        IO.onInitialize();

        ProhibitorModule = new ProhibitorRouter();
        UserConfigModule = new UserConfigRouter();
        KeyValueModule = new KeyValueRouter();
        BridgeModule = new BridgeRouter();
        ResourcerModule = new ResourcerRouter();

        app = Javalin.create((javalinConfig) -> {
                    javalinConfig.showJavalinBanner = false;
                    javalinConfig.jsonMapper(new GsonMapper(gson));
                })
                .start(getConfig().port);

        load(ProhibitorModule);
        load(UserConfigModule);
        load(KeyValueModule);
        load(BridgeModule);
        load(ResourcerModule);
        ResourcerModule.module.injectTranslations();

        System.out.println("LEMBackend server started");
    }

    public static void shutdown() {
        System.out.println("LEMBackend saving all...");

        app.stop();

        save(ProhibitorModule);
        save(UserConfigModule);
        save(KeyValueModule);
        save(BridgeModule);
        save(ResourcerModule);

        System.out.println("LEMBackend all saved");
        IO.stop();
    }

    public static ServerConfig getConfig() {
        return config;
    }

    public static Path getBaseConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("LEMBackend");
    }

    public static boolean secretsMatch(String secret) {
        return getConfig().secretKey.equals(secret);
    }

    private static void load(ModuleRouter<?> router) {
        router.addRoutes();
        router.module.load();
    }

    private static void save(ModuleRouter<?> router) {
        router.module.save();
    }

    @Override
    public void onInitialize() {
        start();
        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(Identifier.of("lembackend", "start"), Event.DEFAULT_PHASE);
        ServerLifecycleEvents.SERVER_STARTED.register(Identifier.of("lembackend", "start"), server -> LEMBackend.minecraftServer = server);

        ServerLifecycleEvents.SERVER_STOPPED.addPhaseOrdering(Event.DEFAULT_PHASE, Identifier.of("lembackend", "stop"));
        ServerLifecycleEvents.SERVER_STOPPED.register(Identifier.of("lembackend", "stop"), server -> LEMBackend.shutdown());
    }
}

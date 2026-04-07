package net.kyrptonaught.LEMBackend;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.LEMBackend.config.ConfigManager;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeRouter;
import net.kyrptonaught.LEMBackend.keyValueStorage.KeyValueRouter;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorRouter;
import net.kyrptonaught.LEMBackend.resourcer.ResourcerRouter;
import net.kyrptonaught.LEMBackend.serverReplay.ServerReplayRouter;
import net.kyrptonaught.LEMBackend.userConfig.UserConfigRouter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

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
    public static ServerReplayRouter ServerReplayModule;

    public static net.kyrptonaught.LEMBackend.legacy.userConfig.UserConfigRouter LegacyUserConfigModule;

    public static void start() {
        config = ConfigManager.load(getBaseConfigPath().resolve("LEMBackendConfig.json"), new ServerConfig());

        IO.onInitialize();

        ProhibitorModule = new ProhibitorRouter();
        UserConfigModule = new UserConfigRouter();
        KeyValueModule = new KeyValueRouter();
        BridgeModule = new BridgeRouter();
        ResourcerModule = new ResourcerRouter();
        ServerReplayModule = new ServerReplayRouter();

        LegacyUserConfigModule = new net.kyrptonaught.LEMBackend.legacy.userConfig.UserConfigRouter();


        app = Javalin.create((config) -> {
                    config.startup.showJavalinBanner = false;
                    config.concurrency.useVirtualThreads = true;
                    config.jsonMapper(new GsonMapper(gson));

                    load(config.routes, ProhibitorModule);
                    load(config.routes, UserConfigModule);
                    load(config.routes, KeyValueModule);
                    load(config.routes, BridgeModule);
                    load(config.routes, ResourcerModule);
                    load(config.routes, ServerReplayModule);
                    load(config.routes, LegacyUserConfigModule);
                })
                .start(getConfig().port);

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
        save(ServerReplayModule);
        save(LegacyUserConfigModule);

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

    private static void load(RoutesConfig config, ModuleRouter<?> router) {
        router.addRoutes(config);
        router.module.load();
    }

    private static void save(ModuleRouter<?> router) {
        router.module.save();
    }

    @Override
    public void onInitialize() {
        start();
        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(Identifier.fromNamespaceAndPath("lembackend", "start"), Event.DEFAULT_PHASE);
        ServerLifecycleEvents.SERVER_STARTED.register(Identifier.fromNamespaceAndPath("lembackend", "start"), server -> LEMBackend.minecraftServer = server);

        ServerLifecycleEvents.SERVER_STOPPED.addPhaseOrdering(Event.DEFAULT_PHASE, Identifier.fromNamespaceAndPath("lembackend", "stop"));
        ServerLifecycleEvents.SERVER_STOPPED.register(Identifier.fromNamespaceAndPath("lembackend", "stop"), server -> LEMBackend.shutdown());
    }
}
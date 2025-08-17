package net.kyrptonaught.LEMBackend;

import com.google.gson.Gson;
import io.javalin.Javalin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.LEMBackend.config.ConfigManager;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeRouter;
import net.kyrptonaught.LEMBackend.keyValueStorage.KeyValueRouter;
import net.kyrptonaught.LEMBackend.linking.LinkRouter;
import net.kyrptonaught.LEMBackend.userConfig.UserConfigRouter;
import net.kyrptonaught.LEMBackend.whitelistSync.WhitelistRouter;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class LEMBackend implements ModInitializer {
    private static ServerConfig config;
    public static Gson gson = ConfigManager.getGSON();
    public static Javalin app;
    public static MinecraftServer minecraftServer;
    public static WhitelistRouter WhitelistModule;
    public static UserConfigRouter UserConfigModule;
    public static LinkRouter LinkingModule;
    public static KeyValueRouter KeyValueModule;
    public static BridgeRouter BridgeModule;

    public static void start(MinecraftServer minecraftServer) {
        LEMBackend.minecraftServer = minecraftServer;
        config = ConfigManager.load(getBaseConfigPath().resolve("LEMBackendConfig.json"), new ServerConfig());

        IO.onInitialize();

        WhitelistModule = new WhitelistRouter();
        UserConfigModule = new UserConfigRouter();
        LinkingModule = new LinkRouter();
        KeyValueModule = new KeyValueRouter();
        BridgeModule = new BridgeRouter();

        app = Javalin.create((javalinConfig) -> {
                    javalinConfig.showJavalinBanner = false;
                    javalinConfig.jsonMapper(new GsonMapper(gson));
                })
                .start(getConfig().port);

        load(WhitelistModule);
        load(UserConfigModule);
        load(LinkingModule);
        load(KeyValueModule);
        load(BridgeModule);

        System.out.println("LEMBackend server started");
    }

    public static void shutdown() {
        System.out.println("LEMBackend saving all...");

        app.stop();

        save(WhitelistModule);
        save(UserConfigModule);
        save(LinkingModule);
        save(KeyValueModule);
        save(BridgeModule);

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
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LEMBackend.shutdown());
        ServerLifecycleEvents.SERVER_STARTED.register(LEMBackend::start);
    }
}

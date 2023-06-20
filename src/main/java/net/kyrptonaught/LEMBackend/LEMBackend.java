package net.kyrptonaught.LEMBackend;

import com.google.gson.Gson;
import io.javalin.Javalin;
import net.kyrptonaught.LEMBackend.advancements.AdvancementModule;
import net.kyrptonaught.LEMBackend.advancements.AdvancementRouter;
import net.kyrptonaught.LEMBackend.config.ServerConfig;
import net.kyrptonaught.LEMBackend.config.api.ConfigManager;
import net.kyrptonaught.LEMBackend.keyValueStorage.KeyValueModule;
import net.kyrptonaught.LEMBackend.keyValueStorage.KeyValueRouter;
import net.kyrptonaught.LEMBackend.linking.LinkRouter;
import net.kyrptonaught.LEMBackend.linking.LinkingModule;
import net.kyrptonaught.LEMBackend.userConfig.UserConfigModule;
import net.kyrptonaught.LEMBackend.userConfig.UserConfigRouter;
import net.kyrptonaught.LEMBackend.whitelistSync.WhitelistModule;
import net.kyrptonaught.LEMBackend.whitelistSync.WhitelistRouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class LEMBackend {
    public static ConfigManager config = new ConfigManager.MultiConfigManager("LEMBackend");
    public static Gson gson = config.gson;
    public static Javalin app;

    public static void start() {
        config.setDir(Paths.get("data"));
        config.registerFile("config", new ServerConfig());
        config.load(true);

        Mod[] modules = new Mod[]{
                new Mod(new WhitelistModule(), new WhitelistRouter()),
                new Mod(new UserConfigModule(), new UserConfigRouter()),
                new Mod(new LinkingModule(), new LinkRouter()),
                new Mod(new KeyValueModule(), new KeyValueRouter()),
                new Mod(new AdvancementModule(), new AdvancementRouter()),
        };

        app = Javalin.create((javalinConfig) -> {
                    javalinConfig.showJavalinBanner = false;
                    javalinConfig.jsonMapper(new GsonMapper(gson));
                })
                .start(getConfig().port);

        for (Mod module : modules) {
            module.module.load(gson);
            module.router.addRoutes();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    app.close();
                    System.out.println("Exited, force saving all...");

                    for (Mod module : modules)
                        module.module.save(gson);

                    System.out.println("Saved");
                }, "Shutdown-thread")
        );

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Server listening. Use 'stop' to quit");

        while (true) {
            String input = null;
            try {
                input = br.readLine();
            } catch (IOException ignored) {
            }
            if (input != null && input.equalsIgnoreCase("stop")) {
                System.exit(0);
                return;
            }
        }
    }

    public static ServerConfig getConfig() {
        return (ServerConfig) config.getConfig("config");
    }

    public static boolean secretsMatch(String secret) {
        return getConfig().secretKey.equals(secret);
    }

    public static class Mod {
        Module module;
        ModuleRouter router;

        public Mod(Module module, ModuleRouter router) {
            this.module = module;
            this.router = router;
            this.router.setModule(module);
        }
    }
}

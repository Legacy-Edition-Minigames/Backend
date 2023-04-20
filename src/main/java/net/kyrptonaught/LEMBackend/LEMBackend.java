package net.kyrptonaught.LEMBackend;

import com.google.gson.Gson;
import io.javalin.Javalin;
import net.kyrptonaught.LEMBackend.advancements.AdvancementRouter;
import net.kyrptonaught.LEMBackend.advancements.AdvancmentLoader;
import net.kyrptonaught.LEMBackend.config.ServerConfig;
import net.kyrptonaught.LEMBackend.config.api.ConfigManager;
import net.kyrptonaught.LEMBackend.keyValueStorage.KeyValueHolder;
import net.kyrptonaught.LEMBackend.keyValueStorage.KeyValueRouter;
import net.kyrptonaught.LEMBackend.linking.LinkHolder;
import net.kyrptonaught.LEMBackend.linking.LinkRouter;
import net.kyrptonaught.LEMBackend.linking.SusHolder;
import net.kyrptonaught.LEMBackend.userConfig.UserConfigHolder;
import net.kyrptonaught.LEMBackend.userConfig.UserConfigRouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class LEMBackend {
    public static ConfigManager config = new ConfigManager.MultiConfigManager("LEMBackend");
    public static Gson gson = config.gson;

    public static void start() {
        config.setDir(Paths.get("data"));
        config.registerFile("config", new ServerConfig());
        config.load(true);

        AdvancmentLoader.createDirectories();
        KeyValueHolder.load();
        LinkHolder.load();
        SusHolder.load();
        UserConfigHolder.load();

        Javalin app = Javalin.create((javalinConfig) -> {
                    javalinConfig.showJavalinBanner = false;
                    javalinConfig.jsonMapper(new GsonMapper(gson));
                })
                .start(getConfig().port);

        app.get("/v0/{secret}/getAdvancements/{uuid}", AdvancementRouter::getAdvancements);
        app.get("/v0/{secret}/unloadPlayer/{uuid}", AdvancementRouter::unloadPlayer);
        app.post("/v0/{secret}/addAdvancements/{uuid}", AdvancementRouter::addAdvancement);
        app.post("/v0/{secret}/overwriteAdvancements/{uuid}", AdvancementRouter::overwriteAdvancements);
        app.post("/v0/{secret}/removeAdvancements/{uuid}", AdvancementRouter::removeAdvancement);
        app.get("/v0/{secret}/kvs/set/{id}/{key}/{value}", KeyValueRouter::setValue);
        app.get("/v0/{secret}/kvs/get/{id}/{key}", KeyValueRouter::getValue);
        app.get("/v0/{secret}/kvs/reset/{id}/{key}", KeyValueRouter::resetValue);
        app.post("/v0/{secret}/link/start/{linkid}/{mcuuid}", LinkRouter::startLink);
        app.post("/v0/{secret}/link/finish/{linkid}/{discordid}", LinkRouter::linkPlayer);
        app.post("/v0/{secret}/link/sus/add/{mcuuid}", LinkRouter::addSus);
        app.post("/v0/{secret}/link/sus/remove/{mcuuid}", LinkRouter::removeSus);
        app.get("/v0/{secret}/link/sus/check/{mcuuid}", LinkRouter::checkSus);
        app.get("/v0/{secret}/getUserConfig/{uuid}", UserConfigRouter::getUserConfig);
        app.post("/v0/{secret}/syncUserConfig/{uuid}/{key}/{value}", UserConfigRouter::syncUserConfig);
        app.post("/v0/{secret}/removeUserConfig/{uuid}/{key}", UserConfigRouter::removeUserConfig);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    app.close();
                    System.out.println("Exited, force saving all...");
                    AdvancmentLoader.saveAdvancements();
                    KeyValueHolder.save();
                    LinkHolder.save();
                    SusHolder.save();
                    UserConfigHolder.save();
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
}

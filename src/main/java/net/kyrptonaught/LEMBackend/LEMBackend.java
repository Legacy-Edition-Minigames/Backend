package net.kyrptonaught.LEMBackend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import net.kyrptonaught.LEMBackend.advancements.AdvancementHolder;
import net.kyrptonaught.LEMBackend.advancements.AdvancementRouter;
import net.kyrptonaught.LEMBackend.advancements.AdvancmentLoader;
import net.kyrptonaught.LEMBackend.config.ServerConfig;
import net.kyrptonaught.LEMBackend.config.api.ConfigManager;

import java.nio.file.Paths;

public class LEMBackend {
    public static ConfigManager config = new ConfigManager.MultiConfigManager("LEMBackend");
    public static Gson gson = new GsonBuilder()
            .registerTypeAdapter(AdvancementHolder.class, new AdvancementHolder.Serializer())
            .create();

    public static void start() {
        config.setDir(Paths.get("data"));
        config.registerFile("config", new ServerConfig());
        config.load(true);

        Javalin app = Javalin.create((javalinConfig) -> {
                    javalinConfig.showJavalinBanner = false;
                    javalinConfig.jsonMapper(new GsonMapper(gson));
                })
                .start(getConfig().port);
        app.get("/getAdvancements/{secret}/{uuid}", AdvancementRouter::getAdvancements);
        app.get("/unloadPlayer/{secret}/{uuid}", AdvancementRouter::unloadPlayer);
        app.post("/addAdvancements/{secret}/{uuid}", AdvancementRouter::addAdvancement);
        app.post("/overwriteAdvancements/{secret}/{uuid}", AdvancementRouter::overwriteAdvancements);
        app.post("/removeAdvancements/{secret}/{uuid}", AdvancementRouter::removeAdvancement);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Exited, force saving all...");
            AdvancmentLoader.saveAdvancements();
            System.out.println("Saved");
                }, "Shutdown-thread")
        );
    }

    public static ServerConfig getConfig() {
        return (ServerConfig) config.getConfig("config");
    }
}

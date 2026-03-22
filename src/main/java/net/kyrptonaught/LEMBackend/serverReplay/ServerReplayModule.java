package net.kyrptonaught.LEMBackend.serverReplay;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.javalin.http.UploadedFile;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.Module;
import net.minecraft.util.LenientJsonParser;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ServerReplayModule extends Module {

    public ServerReplayModule() {
        super("ServerReplays");
    }

    public void upload(String json, UploadedFile file) {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        JsonObject obj = LenientJsonParser.parse(json).getAsJsonObject();

        String id = obj.get("game_id").getAsString();

        try {
            FileHelper.createDir(savePath.resolve("GAMES").resolve(date));
            Files.copy(file.content(), savePath.resolve("GAMES").resolve(date).resolve(id + file.extension()));
            Files.writeString(savePath.resolve("GAMES").resolve(date).resolve(id + ".json"), json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (JsonElement player : obj.get("players").getAsJsonArray()) {
            String uuid = player.getAsJsonObject().get("uuid").getAsString();
            try {
                Files.writeString(savePath.resolve("PLAYER").resolve(uuid), id + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void load(Gson gson) {
        createDirectories();
        FileHelper.createDir(savePath.resolve("GAMES"));
        FileHelper.createDir(savePath.resolve("PLAYER"));
    }
}
package net.kyrptonaught.LEMBackend.userConfig.discordCommands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LegacyImportAdvancementCommand {
    public static String ID = "userconfiglegacy_advimport:";

    public static void execute(SlashCommandInteraction event) {
        event.replyModal(buildModal()).queue();
    }

    private static Modal buildModal() {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        container.add(Label.of("Upload Advancements.zip: ", AttachmentUpload.create("file").setMaxValues(1).setRequired(false).build()));

        return Modal.create(ID + "_submit", "Import Advancements").addComponents(container).build();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        List<Message.Attachment> attachments = event.getValue("file").getAsAttachmentList();


        Path save = LEMBackend.minecraftServer.getWorldPath(LevelResource.ROOT).resolve("advancements.zip");
        Path out = LEMBackend.minecraftServer.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);

        if (!attachments.isEmpty()) {
            FileHelper.download(attachments.get(0).getUrl(), save);
            FileHelper.unzipDirectory(out, save, "advancements/");
            FileHelper.deleteFilePath(save);
        }

        try (Stream<Path> files = Files.walk(out, 1).parallel()) {
            files.forEach(path -> {
                if (!Files.isDirectory(path) && path.toString().endsWith(".json")) {
                    try {
                        String uuid = path.getFileName().toString().replace(".json", "");
                        JsonObject obj = LEMBackend.LegacyUserConfigModule.module.loadPlayer(uuid);
                        JsonArray obj2 = new JsonArray();
                        JsonObject obj3 = LEMBackend.gson.fromJson(FileHelper.readFile(path), JsonObject.class);

                        for (String key : obj3.keySet()) {
                            if (!key.equals("DataVersion") && !key.contains("resource/") && !key.contains("game/combat/")) {
                                JsonObject obj4 = obj3.getAsJsonObject(key).getAsJsonObject("criteria");
                                for (String value : obj4.keySet()) {

                                    obj2.add(key + "__" + value);
                                }
                            }
                        }

                        obj.add("advancements", obj2);

                        LEMBackend.LegacyUserConfigModule.module.syncPlayer(uuid, LEMBackend.gson.toJson(obj));
                    } catch (Exception e) {
                        System.out.println("Failed to import: " + path.getFileName());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        event.getHook().editOriginal("Success!").queue();
    }
}
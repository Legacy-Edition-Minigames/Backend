package net.kyrptonaught.LEMBackend.prohibitor.discordCommands.imports;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.IO;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.ID_TYPE;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.util.LenientJsonParser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SusImportCommand {
    public static String ID = "prohibitor_susimport:";

    public static void execute(SlashCommandInteraction event) {
        event.replyModal(buildModal()).queue();
    }

    private static Modal buildModal() {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        container.add(Label.of("Upload sus.json: ", AttachmentUpload.create("file").setRequired(true).setMaxValues(1).build()));

        return Modal.create(ID + "_submit", "Import Sus").addComponents(container).build();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        List<Message.Attachment> attachments = event.getValue("file").getAsAttachmentList();

        String raw = IO.getAlt(attachments.get(0).getUrl());

        JsonArray obj = LenientJsonParser.parse(raw).getAsJsonArray();
        for (JsonElement element : obj) {
            PlayerEntry entry = ProhibitorModule.load(ID_TYPE.UUID, element.getAsString());
            if (entry.sussyStatus == null) {
                entry.sussyStatus = new StampEntry("Legacy/Unknown", "Legacy/Unknown", Instant.now(), "Legacy/Unknown");
                ProhibitorModule.saveEntry(entry);
            }
        }

        event.getHook().editOriginal("Success!").queue();
    }
}

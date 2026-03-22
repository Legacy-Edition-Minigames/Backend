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
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.DiscordLinkEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.ID_TYPE;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.minecraft.util.LenientJsonParser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LinksImportCommand {
    public static String ID = "prohibitor_linkimport:";

    public static void execute(SlashCommandInteraction event) {
        event.replyModal(buildModal()).queue();
    }

    private static Modal buildModal() {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        container.add(Label.of("Upload links.json: ", AttachmentUpload.create("file").setRequired(true).setMaxValues(1).build()));

        return Modal.create(ID + "_submit", "Import Links").addComponents(container).build();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        List<Message.Attachment> attachments = event.getValue("file").getAsAttachmentList();

        String raw = IO.getAlt(attachments.get(0).getUrl());

        JsonArray obj = LenientJsonParser.parse(raw).getAsJsonArray();
        for (JsonElement element : obj) {
            String uuid = element.getAsJsonObject().get("mcUUID").getAsString();
            long discord = element.getAsJsonObject().get("discordID").getAsLong();

            PlayerEntry entry = ProhibitorModule.load(ID_TYPE.UUID, uuid);
            if (entry.discordLink == null) {
                Instant when = LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(element.getAsJsonObject().get("dateLinked").getAsString())).atZone(ZoneId.systemDefault()).toInstant();
                entry.discordLink = new DiscordLinkEntry(when, discord, "Legacy/Unknown");
                LinkingManager.addLink(uuid, discord);
                ProhibitorModule.saveEntry(entry);
            }
        }
        LEMBackend.ProhibitorModule.module.save(LEMBackend.gson);

        event.getHook().editOriginal("Success!").queue();
    }
}

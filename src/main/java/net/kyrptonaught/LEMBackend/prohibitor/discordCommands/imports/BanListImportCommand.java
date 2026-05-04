package net.kyrptonaught.LEMBackend.prohibitor.discordCommands.imports;


import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.IO;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.BanEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.ID_TYPE;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BanListImportCommand {
    public static String ID = "prohibitor_banlistimport:";

    public static void execute(SlashCommandInteraction event) {
        event.replyModal(buildModal()).queue();
    }

    private static Modal buildModal() {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        container.add(Label.of("Upload history.csv: ", AttachmentUpload.create("file").setRequired(true).setMaxValues(1).build()));

        return Modal.create(ID + "_submit", "Import Ban List").addComponents(container).build();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        List<Message.Attachment> attachments = event.getValue("file").getAsAttachmentList();

        String[] raw = IO.getAlt(attachments.get(0).getUrl()).split("\r\n");

        for (int i = 1; i < raw.length; i++) {
            String[] data = raw[i].replaceAll(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", ",,,").replaceAll("\"", "").split(",,,");
            String uuid = data[1];
            String ip = data[2];
            String name = data[3];

            String who = data[5];
            Instant when = Instant.ofEpochSecond(Long.parseLong(data[7]));
            int duration = Integer.parseInt(data[8]);
            String reason = data[9];

            String type = data[10];

            ProhibitorModule.getJoinStatus(uuid, name, ip, "", "NONE", false);

            PlayerEntry uuidEntry = ProhibitorModule.load(ID_TYPE.UUID, uuid);
            PlayerEntry ipEntry = ProhibitorModule.load(ID_TYPE.IP, ip);
            StampEntry stamp = new StampEntry(who, "BanHammerDB", when, reason);

            switch (type) {
                case "ban" -> {
                    if (duration < 0) uuidEntry.addBan(BanEntry.PermaBan(stamp));
                    else uuidEntry.addBan(BanEntry.TempBan(stamp, duration, (byte) ChronoUnit.SECONDS.ordinal()));
                }
                case "ipban" -> {
                    if (duration < 0) ipEntry.addBan(BanEntry.PermaBan(stamp));
                    else ipEntry.addBan(BanEntry.TempBan(stamp, duration, (byte) ChronoUnit.SECONDS.ordinal()));
                }
                case "mute" -> {
                    if (duration < 0) uuidEntry.addMute(BanEntry.PermaBan(stamp));
                    else uuidEntry.addMute(BanEntry.TempBan(stamp, duration, (byte) ChronoUnit.SECONDS.ordinal()));
                }
                case "kick" -> uuidEntry.addKick(stamp);
                case "warn" -> uuidEntry.addWarn(stamp);
                default -> System.out.println(raw[i]);
            }

            if (!uuid.equals("00000000-0000-0000-0000-000000000000")) ProhibitorModule.saveEntry(uuidEntry);
            if (!ip.isEmpty() && !ip.equals("unknown")) ProhibitorModule.saveEntry(ipEntry);
        }

        event.getHook().editOriginal("Success!").queue();
    }
}
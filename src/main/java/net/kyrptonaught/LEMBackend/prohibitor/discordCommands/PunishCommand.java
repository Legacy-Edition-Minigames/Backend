package net.kyrptonaught.LEMBackend.prohibitor.discordCommands;

import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeModule;
import net.kyrptonaught.LEMBackend.prohibitor.Configs;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorDiscordCommands;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.actions.*;
import net.kyrptonaught.LEMBackend.prohibitor.entries.ID_TYPE;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class PunishCommand {
    public static String ID = "prohibitor_punish:";

    public static void execute(SlashCommandInteraction event) {
        event.deferReply(true).queue();
        event.getHook().sendMessageComponents(Container.of(buildMessage("player_name", "indefinite", "other"))).useComponentsV2().setEphemeral(true).queue();
    }

    public static void selectInteraction(StringSelectInteractionEvent event) {
        String[] key = event.getInteraction().getValues().getFirst().split("---");
        event.editComponents(Container.of(buildMessage(key[1], key[2], key[3]))).useComponentsV2().queue();
    }

    public static void buttonInteraction(ButtonInteractionEvent event) {
        if (event.getButton().getCustomId().contains("_viewskin_")) event.replyModal(buildRequestSkin(event.getButton().getCustomId())).queue();
        else {
            Optional<StringSelectMenu> playerID = event.getInteraction().getMessage().getComponentTree().find(StringSelectMenu.class, stringSelectMenu -> stringSelectMenu.getCustomId().equals("intermediary_player_id"));
            if (playerID.isPresent()) event.replyModal(buildModal(event.getButton().getCustomId(), playerID.get().getOptions().getFirst().getValue())).queue();
            else event.replyModal(buildModal(event.getButton().getCustomId(), null)).queue();
        }
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferEdit().queue();
        String id = event.getCustomId();

        String player_uuid = null;

        if (id.contains("player_uuid")) {
            player_uuid = event.getValue("player_id").getAsString();
        } else if (id.contains("player_name")) {
            player_uuid = ProhibitorModule.getUUIDFromName(event.getValue("player_id").getAsString());
        } else if (id.contains("player_discord")) {
            long discord = event.getValue("player_id").getAsLongList().getFirst();
            player_uuid = LinkingManager.getMCFromDiscord(discord);
        }

        if (id.contains("_viewskin_")) {
            event.getHook().sendMessageComponents(Container.of(buildViewSkin(player_uuid))).useComponentsV2().setEphemeral(true).queue();
            return;
        }

        if (id.contains("_action_submit")) {
            String banDuration = event.getValue(ID + "ban_duration").getAsStringList().getFirst();
            String reasonPreset = event.getValue(ID + "reason_preset").getAsStringList().getFirst();
            String punishType = event.getValue(ID + "punishment_type").getAsStringList().getFirst();
            event.getHook().sendMessageComponents(Container.of(buildIntermediary(player_uuid, punishType, banDuration.split("---")[2], reasonPreset.split("---")[3]))).useComponentsV2().setEphemeral(true).queue();
            return;
        }

        String who = event.getMember().getEffectiveName() + " - Discord";
        String source = event.getGuild().getName() + " - Discord";
        String reason = event.getValue("reason").getAsString();

        List<Message.Attachment> attachments = event.getValue("evidence").getAsAttachmentList();
        String[] evidence = new String[attachments.size()];

        for (int i = 0; i < attachments.size(); i++) {
            evidence[i] = ProhibitorModule.downloadEvidence(attachments.get(i).getUrl(), player_uuid, attachments.get(i).getFileExtension());
        }

        if (id.contains("player_ip")) {
            player_uuid = event.getValue("player_id").getAsString();
            if (id.contains("_ban_")) {
                if (id.contains("indefinite")) {
                    BanAction.permaBan(ID_TYPE.IP, player_uuid, who, source, reason, evidence);
                } else {
                    int durationTime = Integer.parseInt(event.getValue("duration_time").getAsString());
                    byte durationType = Byte.parseByte(id.split("---")[2].split("_")[2]);
                    BanAction.tempBan(ID_TYPE.IP, player_uuid, who, source, reason, durationTime, durationType, evidence);
                }
            }
        } else {
            if (id.contains("_ban_")) {
                String id_type = String.join("-", event.getValue(ID + "punishment_id_type").getAsStringList());
                if (id.contains("player_ip")) id_type = "_ip_";

                if (id.contains("indefinite")) {
                    BanAction.multiPermBan(id_type, player_uuid, who, source, reason, evidence);
                } else {
                    int durationTime = Integer.parseInt(event.getValue("duration_time").getAsString());
                    byte durationType = Byte.parseByte(id.split("---")[2].split("_")[1]);
                    BanAction.multiTempBan(id_type, player_uuid, who, source, reason, durationTime, durationType, evidence);
                }
            } else if (id.contains("_mute_")) {
                if (id.contains("indefinite")) MuteAction.permaMute(player_uuid, who, source, reason, evidence);
                else {
                    int durationTime = Integer.parseInt(event.getValue("duration_time").getAsString());
                    byte durationType = Byte.parseByte(id.split("---")[2].split("_")[1]);
                    MuteAction.tempMute(player_uuid, who, source, reason, durationTime, durationType, evidence);
                }
            } else if (id.contains("_kick_")) KickAction.kick(player_uuid, who, source, reason, evidence);
            else if (id.contains("_warn_")) WarnAction.warn(player_uuid, who, source, reason, evidence);
            else if (id.contains("_sus_")) SusAction.sus(player_uuid, who, source, reason, evidence);
            else if (id.contains("_whitelist_")) WhitelistAction.whitelist(player_uuid, who, source, reason);
            else if (id.contains("_skinban_")) SkinBanAction.skinBan(player_uuid, who, source, reason, evidence);
        }

        event.getHook().sendMessage("Success!").setEphemeral(true).queue();
    }

    private static List<ContainerChildComponent> buildMessage(String playerLookupType, String durationType, String reasonPreset) {
        List<ContainerChildComponent> container = new ArrayList<>();
        String key = "---" + playerLookupType + "---" + durationType + "---" + reasonPreset + "---";

        container.add(ProhibitorDiscordCommands.getTitle("Issue a Punishment"));
        container.add(Separator.createInvisible(Separator.Spacing.LARGE));

        container.add(TextDisplay.of("Player ID Type:"));
        container.add(ActionRow.of(StringSelectMenu.create(ID + "player_id_type")
                .addOption("MC Name", "---" + "player_name" + "---" + durationType + "---" + reasonPreset + "---")
                .addOption("MC UUID", "---" + "player_uuid" + "---" + durationType + "---" + reasonPreset + "---")
                .addOption("Discord", "---" + "player_discord" + "---" + durationType + "---" + reasonPreset + "---")
                .addOption("IP (Only For Bans)", "---" + "player_ip" + "---" + durationType + "---" + reasonPreset + "---")
                .setPlaceholder("Player ID Type")
                .setDefaultValues(key)
                .setRequired(true).build()));

        container.add(TextDisplay.of("Punishment Duration:"));
        container.add(TextDisplay.of("-# Applicable to Bans/Mutes"));
        container.add(ActionRow.of(durationSelect(key, playerLookupType, reasonPreset).setRequired(true).build()));

        container.add(TextDisplay.of("Punishment Reason Preset:"));
        container.add(ActionRow.of(reasonSelect(key, playerLookupType, durationType).setRequired(true).build()));

        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        if (playerLookupType.contains("player_ip")) {
            container.add(ActionRow.of(Button.primary(ID + "button_ban_" + key, "Ban")));
        } else {

            container.add(TextDisplay.of("Ban/Mute"));
            container.add(ActionRow.of(Button.primary(ID + "button_ban_" + key, "Ban"), Button.primary(ID + "button_mute_" + key, "Mute"), Button.primary(ID + "button_skinban_" + key, "Ban Skin")));
            container.add(Separator.createDivider(Separator.Spacing.LARGE));

            container.add(TextDisplay.of("Other"));
            container.add(ActionRow.of(Button.primary(ID + "button_whitelist_" + key, "Whitelist"), Button.primary(ID + "button_warn_" + key, "Warning"), Button.primary(ID + "button_kick_" + key, "Kick"),
                    Button.primary(ID + "button_sus_" + key, "Sus"), Button.primary(ID + "button_viewskin_" + key, "View Skin")));
        }

        return container;
    }

    private static Modal buildModal(String button, String playerID) {
        List<ModalTopLevelComponent> container = new ArrayList<>();
        String[] key = button.split("---");

        if (button.contains("player_name"))
            container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("MC Name").setValue(playerID).setRequired(true).build()));
        else if (button.contains("player_uuid"))
            container.add(Label.of("Player's MC UUID: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("MC UUID").setValue(playerID).setRequired(true).build()));
        else if (button.contains("player_discord"))
            container.add(Label.of("Player's Discord: ", discordSelect(playerID).setRequired(true).build()));
        else if (button.contains("player_ip"))
            container.add(Label.of("Player's IP: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("IP").setValue(playerID).setRequired(true).build()));

        if (button.contains("_ban_") && !button.contains("player_ip")) {
            container.add(Label.of("Ban Type: ", StringSelectMenu.create(ID + "punishment_id_type")
                    .addOption("UUID", "_uuid_")
                    .addOption("IP", "_ip_")
                    .addOption("Name", "_name_")
                    .setDefaultValues("_uuid_", "_ip_").setRequiredRange(1, 3).setRequired(true).build()));
        }

        if (!button.contains("indefinite") && (button.contains("button_ban_") || button.contains("button_mute_"))) {
            String desc = ChronoUnit.values()[Byte.parseByte(key[2].split("_")[1])].toString().replace("s", "(s)");
            container.add(Label.of(desc + ": ", TextInput.create("duration_time", TextInputStyle.SHORT).setPlaceholder("Duration").setRequired(true).build()));
        }

        container.add(Label.of("Reason: ", TextInput.create("reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setValue(punishmentPresets().get(key[3])).setRequired(true).build()));

        container.add(Label.of("Evidence: ", AttachmentUpload.create("evidence").setRequired(false).setMaxValues(10).build()));

        return Modal.create(button + "_submit", getModalLabel(button)).addComponents(container).build();
    }

    public static Modal buildActionModal(String playerLookupType, String playerID) {
        String key = "---" + playerLookupType + "---" + "indefinite" + "---" + "other" + "---";
        List<ModalTopLevelComponent> container = new ArrayList<>();

        if (playerLookupType.contains("player_name"))
            container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(playerID).setRequired(true).build()));
        else if (playerLookupType.contains("player_uuid"))
            container.add(Label.of("Player's MC UUID: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(playerID).setRequired(true).build()));
        else if (playerLookupType.contains("player_discord"))
            container.add(Label.of("Player's Discord: ", discordSelect(playerID).setRequired(true).build()));
        else if (playerLookupType.contains("player_ip"))
            container.add(Label.of("Player's IP: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(playerID).setRequired(true).build()));

        container.add(Label.of("Punishment Type", StringSelectMenu.create(ID + "punishment_type")
                .addOption("Perm Ban", "_ban_indefinite_")
                .addOption("Temp Ban", "_ban_")
                .addOption("Perm Mute", "_mute_indefinite_")
                .addOption("Temp Mute", "_mute_")
                .addOption("Skin Ban", "_skinban_")
                .addOption("Whitelist", "_whitelist_")
                .addOption("Warning", "_warn_")
                .addOption("Kick", "_kick_")
                .addOption("Sus", "_sus_")
                .setRequired(true).build()));

        container.add(Label.of("Punishment Duration:", durationSelect(key, playerLookupType, "other").setRequired(true).build()));
        container.add(Label.of("Punishment Reason Preset:", reasonSelect(key, playerLookupType, "indefinite").setRequired(true).build()));

        return Modal.create(ID + key + "_action_submit", "Issue a Punishment").addComponents(container).build();
    }

    private static List<ContainerChildComponent> buildIntermediary(String playerID, String punishmentType, String durationType, String reasonPreset) {
        List<ContainerChildComponent> container = new ArrayList<>();
        String key = "---" + "player_uuid" + "---" + durationType + "---" + reasonPreset + "---";

        container.add(ProhibitorDiscordCommands.getTitle("Issue a Punishment"));
        container.add(Separator.createInvisible(Separator.Spacing.SMALL));

        container.add(TextDisplay.of("Player's Name: " + ProhibitorModule.getNameFromUUID(playerID)));

        container.add(TextDisplay.of("Player's UUID: "));
        container.add(ActionRow.of(StringSelectMenu.create("intermediary_player_id").addOption(playerID, playerID).setDefaultValues(playerID).setRequired(true).setDisabled(true).build()));
        container.add(Separator.createInvisible(Separator.Spacing.SMALL));

        container.add(ActionRow.of(Button.primary(ID + "button" + punishmentType + key, getModalLabel(punishmentType))));

        return container;
    }

    private static Modal buildRequestSkin(String button) {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        if (button.contains("player_name"))
            container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("MC Name").setRequired(true).build()));
        else if (button.contains("player_uuid"))
            container.add(Label.of("Player's MC UUID: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("MC UUID").setRequired(true).build()));
        else if (button.contains("player_discord"))
            container.add(Label.of("Player's Discord: ", EntitySelectMenu.create("player_id", EntitySelectMenu.SelectTarget.USER).setRequired(true).build()));
        else if (button.contains("player_ip"))
            container.add(Label.of("Player's IP: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("IP").setRequired(true).build()));

        return Modal.create(button + "_submit", "View a Player's Skin").addComponents(container).build();
    }

    private static List<ContainerChildComponent> buildViewSkin(String uuid) {
        List<ContainerChildComponent> container = new ArrayList<>();
        container.add(ProhibitorDiscordCommands.getTitle("View Skin"));
        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        String url = SkinBanAction.getPlayerSkin(uuid);

        container.add(TextDisplay.of("## *" + uuid + "*'s skin"));
        SkinBanAction.generateSkinRender(url);
        container.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(ProhibitorModule.getSkinRenderPath(url))).withDescription(url)));
        return container;
    }

    private static String getModalLabel(String button) {
        StringBuilder out = new StringBuilder();
        if (button.contains("_kick_")) out.append("Kick ");
        else if (button.contains("_warn_")) out.append("Warn ");
        else if (button.contains("_whitelist_")) out.append("Whitelist ");
        else if (button.contains("_sus_")) out.append("Sus ");
        else if (button.contains("_skinban_")) out.append("Skin Ban ");
        else {
            if (button.contains("indefinite")) out.append("Permanently ");
            else out.append("Temporarily ");

            if (button.contains("_ban_")) out.append("Ban ");
            else if (button.contains("_mute_")) out.append("Mute ");
        }

        out.append("a Player");
        return out.toString();
    }

    private static StringSelectMenu.Builder durationSelect(String key, String playerLookupType, String reasonPreset) {
        return StringSelectMenu.create(ID + "ban_duration")
                .addOption("Indefinite", "---" + playerLookupType + "---" + "indefinite" + "---" + reasonPreset + "---")
                .addOption("Minute(s)", "---" + playerLookupType + "---" + "minute_" + ChronoUnit.MINUTES.ordinal() + "---" + reasonPreset + "---")
                .addOption("Hour(s)", "---" + playerLookupType + "---" + "hour_" + ChronoUnit.HOURS.ordinal() + "---" + reasonPreset + "---")
                .addOption("Day(s)", "---" + playerLookupType + "---" + "day_" + ChronoUnit.DAYS.ordinal() + "---" + reasonPreset + "---")
                .addOption("Month(s)", "---" + playerLookupType + "---" + "month_" + ChronoUnit.MONTHS.ordinal() + "---" + reasonPreset + "---")
                .addOption("Year(s)", "---" + playerLookupType + "---" + "year_" + ChronoUnit.YEARS.ordinal() + "---" + reasonPreset + "---")
                .setPlaceholder("Duration")
                .setDefaultValues(key);
    }

    private static StringSelectMenu.Builder reasonSelect(String key, String playerLookupType, String durationType) {
        StringSelectMenu.Builder stringSelect = StringSelectMenu.create(ID + "reason_preset").setPlaceholder("Reason");

        for (String presetKey : punishmentPresets().keySet()) {
            stringSelect.addOption(punishmentPresets().get(presetKey), "---" + playerLookupType + "---" + durationType + "---" + presetKey + "---");
        }

        stringSelect.setDefaultValues(key);
        return stringSelect;
    }

    public static EntitySelectMenu.Builder discordSelect(String playerID) {
        EntitySelectMenu.Builder builder = EntitySelectMenu.create("player_id", EntitySelectMenu.SelectTarget.USER);
        if (playerID != null) builder.setDefaultValues(EntitySelectMenu.DefaultValue.from(BridgeModule.jda.getUserById(playerID)));
        return builder;
    }

    private static HashMap<String, String> punishmentPresets() {
        return Configs.punishmentPresets;
    }
}
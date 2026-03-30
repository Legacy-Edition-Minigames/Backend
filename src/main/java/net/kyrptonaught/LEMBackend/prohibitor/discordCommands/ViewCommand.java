package net.kyrptonaught.LEMBackend.prohibitor.discordCommands;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorDiscordCommands;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.actions.Actions;
import net.kyrptonaught.LEMBackend.prohibitor.actions.SkinBanAction;
import net.kyrptonaught.LEMBackend.prohibitor.entries.*;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

public class ViewCommand {
    public static String ID = "prohibitor_view:";

    public static void execute(SlashCommandInteraction event) {
        event.deferReply(true).queue();
        event.getHook().sendMessageComponents(Container.of(buildMessage("player_name"))).useComponentsV2().queue();
    }

    public static void selectInteraction(StringSelectInteractionEvent event) {
        event.editComponents(Container.of(buildMessage(event.getInteraction().getValues().getFirst()))).useComponentsV2().queue();
    }

    public static void buttonInteraction(ButtonInteractionEvent event) {
        String id = event.getButton().getCustomId();
        if (id.startsWith(ID + "open")) {
            String[] key = id.split(",");
            event.editComponents(buildHistory(key[1], Integer.parseInt(key[2]))).useComponentsV2().queue();
        } else event.replyModal(buildSelectModal(id, null)).queue();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferReply(true).queue();
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

        event.getHook().sendMessageComponents(buildHistory(player_uuid, 0)).useComponentsV2().queue();
    }

    private static List<ContainerChildComponent> buildMessage(String playerLookupType) {
        List<ContainerChildComponent> container = new ArrayList<>();
        container.add(ProhibitorDiscordCommands.getTitle("Lookup Player Punishments"));
        container.add(Separator.createInvisible(Separator.Spacing.SMALL));

        container.add(TextDisplay.of("Player ID Type:"));
        container.add(ActionRow.of(StringSelectMenu.create(ID + "player_id_type")
                .addOption("MC Name", "player_name")
                .addOption("MC UUID", "player_uuid")
                .addOption("Discord", "player_discord")
                .addOption("IP (Only For Bans)", "player_ip")
                .setPlaceholder("Player ID Type")
                .setDefaultValues(playerLookupType)
                .setRequired(true).build()));

        container.add(Separator.createInvisible(Separator.Spacing.LARGE));
        container.add(ActionRow.of(Button.primary(ID + "button_view_" + playerLookupType, "View")));
        return container;
    }

    public static Modal buildSelectModal(String button, String id) {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        if (button.contains("player_name"))
            container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(id).setPlaceholder("MC Name").setRequired(true).build()));
        else if (button.contains("player_uuid"))
            container.add(Label.of("Player's MC UUID: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(id).setPlaceholder("MC UUID").setRequired(true).build()));
        else if (button.contains("player_discord"))
            container.add(Label.of("Player's Discord: ", PunishCommand.discordSelect(id).setRequired(true).build()));
        else if (button.contains("player_ip"))
            container.add(Label.of("Player's IP: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(id).setPlaceholder("IP").setRequired(true).build()));

        return Modal.create(button + "_submit", "Lookup").addComponents(container).build();
    }

    public static Container buildHistory(String uuid, int page) {
        List<ContainerChildComponent> container = new ArrayList<>();
        Instant now = Instant.now();
        PlayerEntry playerEntry = ProhibitorModule.loadUUID(uuid);

        container.add(Section.of(Button.secondary("test", Emoji.fromUnicode("⌚")), TextDisplay.of("## *" + playerEntry.associatedName + "*'s history")));
        container.add(TextDisplay.of("-# **UUID:** " + playerEntry.associatedUUID));
        if (playerEntry.firstSeen == null || playerEntry.lastSeen == null) {
            container.add(TextDisplay.of("-# **First Join:** Never"));
            container.add(TextDisplay.of("-# **Last Join:** Never"));
        } else {
            container.add(TextDisplay.of("-# **First Join:** " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(playerEntry.firstSeen)));
            container.add(TextDisplay.of("-# **Last Join:** " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(playerEntry.lastSeen)));
        }
        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        int currentPage = -1;
        int totalPages = 0;

        if (playerEntry.associatedIP != null) {
            totalPages++;
            if (currentPage != -999)
                if (++currentPage == page) {
                    container.add(TextDisplay.of("### IP Info"));
                    buildIPInfo(container, playerEntry.associatedIP);
                    currentPage = -999;
                }
        }

        if (playerEntry.discordLink != null) {
            totalPages++;
            if (currentPage != -999)
                if (++currentPage == page) {
                    buildPunishment(container, Actions.LINK, playerEntry.discordLink);
                    currentPage = -999;
                }
        }

        if (playerEntry.whitelistStatus != null) {
            totalPages++;
            if (currentPage != -999)
                if (++currentPage == page) {
                    buildPunishment(container, Actions.WHITELIST, playerEntry.whitelistStatus);
                    currentPage = -999;
                }
        }
        if (playerEntry.sussyStatus != null) {
            totalPages++;
            if (currentPage != -999)
                if (++currentPage == page) {
                    buildPunishment(container, Actions.SUS, playerEntry.sussyStatus);
                    currentPage = -999;
                }
        }
        if (!playerEntry.bans.isEmpty()) {
            totalPages += playerEntry.bans.size();
            if (currentPage != -999)
                for (BanEntry entry : playerEntry.bans) {
                    if (++currentPage == page) {
                        playerEntry.checkBan(entry, now);
                        buildPunishment(container, Actions.BAN, entry);
                        currentPage = -999;
                        break;
                    }
                }
        }
        if (!playerEntry.mutes.isEmpty()) {
            totalPages += playerEntry.mutes.size();
            if (currentPage != -999)
                for (BanEntry entry : playerEntry.mutes) {
                    if (++currentPage == page) {
                        playerEntry.checkBan(entry, now);
                        buildPunishment(container, Actions.MUTE, entry);
                        currentPage = -999;
                        break;
                    }
                }
        }
        if (!playerEntry.skinBans.isEmpty()) {
            totalPages += playerEntry.skinBans.size();
            if (currentPage != -999)
                for (SkinBanEntry entry : playerEntry.skinBans) {
                    if (++currentPage == page) {
                        buildPunishment(container, Actions.SKINBAN, entry);
                        currentPage = -999;
                        break;
                    }
                }
        }
        if (!playerEntry.warns.isEmpty()) {
            totalPages += playerEntry.warns.size();
            if (currentPage != -999)
                for (StampEntry entry : playerEntry.warns) {
                    if (++currentPage == page) {
                        buildPunishment(container, Actions.WARN, entry);
                        currentPage = -999;
                        break;
                    }
                }
        }
        if (!playerEntry.kicks.isEmpty()) {
            totalPages += playerEntry.kicks.size();
            if (currentPage != -999)
                for (StampEntry entry : playerEntry.kicks) {
                    if (++currentPage == page) {
                        buildPunishment(container, Actions.KICK, entry);
                        currentPage = -999;
                        break;
                    }
                }
        }

        container.add(ActionRow.of(
                Button.of(page > 0 ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY, ID + "open1," + uuid + "," + (page - (page > 0 ? 1 : 0)), Emoji.fromUnicode("⬅️")),
                Button.secondary(ID + "open2," + uuid + "," + (page), (page + 1) + "/" + totalPages),
                Button.of(page + 1 < totalPages ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY, ID + "open3," + uuid + "," + (page + (page + 1 < totalPages ? 1 : 0)), Emoji.fromUnicode("➡️"))
        ));

        return Container.of(container);
    }

    public static void buildPunishment(List<ContainerChildComponent> container, Actions action, Entry entry) {
        switch (action) {
            case BAN -> {
                container.add(TextDisplay.of("### Ban"));
                addDuration(container, (BanEntry) entry);
                addStamp(container, ((BanEntry) entry).banSource);
                addRevoked(container, ((BanEntry) entry).revokedSource);
            }
            case MUTE -> {
                container.add(TextDisplay.of("### Mute"));
                addDuration(container, (BanEntry) entry);
                addStamp(container, ((BanEntry) entry).banSource);
                addRevoked(container, ((BanEntry) entry).revokedSource);
            }
            case SKINBAN -> {
                container.add(TextDisplay.of("### Skin Ban"));
                SkinBanAction.generateSkinRender(((SkinBanEntry) entry).skin);
                container.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(ProhibitorModule.getSkinRenderPath(((SkinBanEntry) entry).skin))).withDescription(((SkinBanEntry) entry).skin)));
                addStamp(container, ((SkinBanEntry) entry).banSource);
            }
            case WARN -> {
                container.add(TextDisplay.of("### Warn"));
                addStamp(container, ((StampEntry) entry));
            }
            case KICK -> {
                container.add(TextDisplay.of("### Kick"));
                addStamp(container, ((StampEntry) entry));
            }
            case WHITELIST -> {
                container.add(TextDisplay.of("### Whitelisted"));
                addStamp(container, ((StampEntry) entry));
            }
            case SUS -> {
                container.add(TextDisplay.of("### Suspicious"));
                addStamp(container, ((StampEntry) entry));
            }
            case LINK -> {
                container.add(TextDisplay.of("### Discord Link"));
                container.add(TextDisplay.of("Discord: " + "<@" + ((DiscordLinkEntry) entry).discordID() + ">"));
                container.add(TextDisplay.of("Date Linked: " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(((DiscordLinkEntry) entry).dateLinked())));
                container.add(TextDisplay.of("Server: " + ((DiscordLinkEntry) entry).server()));
            }
            case UNBAN -> {
                container.add(TextDisplay.of("### Un-Ban"));
                addStamp(container, ((StampEntry) entry));
            }
            case UNMUTE -> {
                container.add(TextDisplay.of("### Un-Mute"));
                addStamp(container, ((StampEntry) entry));
            }
            case UNWHITELIST -> {
                container.add(TextDisplay.of("### Un-Whitelist"));
                addStamp(container, ((StampEntry) entry));
            }
            case UNSUS -> {
                container.add(TextDisplay.of("### Un-SUS"));
                addStamp(container, ((StampEntry) entry));
            }
        }
    }

    private static void addStamp(List<ContainerChildComponent> container, StampEntry stamp) {
        container.add(TextDisplay.of("**When:** " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(stamp.when)));
        container.add(TextDisplay.of("**By:** " + stamp.who));
        container.add(TextDisplay.of("**Reason:** " + stamp.why));
        if (!stamp.evidence.isEmpty()) {
            container.add(TextDisplay.of("**Evidence:** "));
            container.add(MediaGallery.of(stamp.evidence.stream().map(file -> MediaGalleryItem.fromFile(FileUpload.fromData(ProhibitorModule.getEvidiencePath(file)))).toList()));
        }
    }

    private static void addDuration(List<ContainerChildComponent> container, BanEntry entry) {
        if (entry.permanent) container.add(TextDisplay.of("**Expires:** Indefinite"));
        else {
            container.add(TextDisplay.of("**Expires:** " + entry.duration_pretty + (entry.expired ? " - Expired" : "")));
        }
    }

    private static void addRevoked(List<ContainerChildComponent> container, StampEntry stamp) {
        if (stamp == null) return;
        container.add(Separator.createDivider(Separator.Spacing.SMALL));
        container.add(TextDisplay.of("### Revoked"));
        container.add(TextDisplay.of("**When:** " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(stamp.when)));
        container.add(TextDisplay.of("**By:** " + stamp.who));
        container.add(TextDisplay.of("**Reason:** " + stamp.why));
    }

    private static void buildIPInfo(List<ContainerChildComponent> container, String ip) {
        JsonObject obj = FileHelper.download("http://ip-api.com/json/" + ip + "?fields=16991744", JsonObject.class);

        container.add(TextDisplay.of("IP: " + ip));
        if (obj == null) {
            TextDisplay.of("Status: Failed");
            return;
        }

        String status = obj.get("status").getAsString();
        if (!status.equals("success")) {
            TextDisplay.of("Status: " + status);
            return;
        }

        container.add(TextDisplay.of("ISP: " + obj.get("isp").getAsString()));
        container.add(TextDisplay.of("ORG: " + obj.get("org").getAsString()));
        container.add(TextDisplay.of("Mobile: " + obj.get("mobile").getAsString()));
        container.add(TextDisplay.of("Proxy: " + obj.get("proxy").getAsString()));
        container.add(TextDisplay.of("Hosting: " + obj.get("hosting").getAsString()));
    }
}

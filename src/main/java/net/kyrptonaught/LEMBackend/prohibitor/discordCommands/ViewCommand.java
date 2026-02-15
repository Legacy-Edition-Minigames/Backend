package net.kyrptonaught.LEMBackend.prohibitor.discordCommands;

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
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorDiscordCommands;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorExecuter;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.*;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import org.w3c.dom.Text;


import java.nio.file.Path;
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
            event.editComponents(buildHistory(key[1], key[2], Integer.parseInt(key[3]))).useComponentsV2().queue();
        } else event.replyModal(buildSelectModal(id)).queue();
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
        String types = String.join("-", event.getValue(ID + "punishment_type").getAsStringList());


        event.getHook().sendMessageComponents(buildHistory(player_uuid, types, 0)).useComponentsV2().queue();
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

    private static Modal buildSelectModal(String button) {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        if (button.contains("player_name"))
            container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("MC Name").setRequired(true).build()));
        else if (button.contains("player_uuid"))
            container.add(Label.of("Player's MC UUID: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("MC UUID").setRequired(true).build()));
        else if (button.contains("player_discord"))
            container.add(Label.of("Player's Discord: ", EntitySelectMenu.create("player_id", EntitySelectMenu.SelectTarget.USER).setRequired(true).build()));
        else if (button.contains("player_ip"))
            container.add(Label.of("Player's IP: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("IP").setRequired(true).build()));

        container.add(Label.of("Types", StringSelectMenu.create(ID + "punishment_type")
                .addOption("Bans", "_b_")
                .addOption("Mutes", "_m_")
                .addOption("Skin Bans", "_sb_")
                .addOption("Whitelist", "_wl_")
                .addOption("Warnings", "_w_")
                .addOption("Kicks", "_k_")
                .addOption("Sussies", "_ss_")
                .setDefaultValues("_b_").setMaxValues(7)
                .setRequired(true).build()));

        return Modal.create(button + "_submit", "Lookup").addComponents(container).build();
    }

    private static Container buildHistory(String uuid, String types, int page) {
        List<ContainerChildComponent> container = new ArrayList<>();
        Instant now = Instant.now();
        PlayerEntry playerEntry = ProhibitorModule.loadUUID(uuid);

        container.add(Section.of(Button.secondary("test", Emoji.fromUnicode("⌚")), TextDisplay.of("## *" + playerEntry.associatedName + "*'s history")));
        container.add(TextDisplay.of("-# **UUID:** " + playerEntry.associatedUUID));
        container.add(TextDisplay.of("-# **IP:** " + playerEntry.associatedIP));
        container.add(TextDisplay.of("-# **First Join:** " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(playerEntry.firstSeen)));
        container.add(TextDisplay.of("-# **Last Join:** " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(playerEntry.lastSeen)));

        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        int currentPage = -1;
        int totalPages = 0;


        if (types.contains("_wl_") && playerEntry.whitelistStatus != null) {
            totalPages++;
            if (currentPage != -999)
                if (++currentPage == page) {
                    container.add(TextDisplay.of("### Whitelisted"));
                    addStamp(container, playerEntry.whitelistStatus);
                    currentPage = -999;
                }
        }
        if (types.contains("_ss_") && playerEntry.sussyStatus != null) {
            totalPages++;
            if (currentPage != -999)
                if (++currentPage == page) {
                    container.add(TextDisplay.of("### Suspicious"));
                    addStamp(container, playerEntry.sussyStatus);
                    currentPage = -999;
                }
        }
        if (types.contains("_b_")) {
            totalPages += playerEntry.bans.size();
            if (currentPage != -999)
                for (BanEntry entry : playerEntry.bans) {
                    if (++currentPage == page) {
                        playerEntry.checkBan(entry, now);
                        container.add(TextDisplay.of("### Ban"));
                        addDuration(container, entry);
                        addStamp(container, entry.banSource);
                        addRevoked(container, entry.revokedSource);
                        currentPage = -999;
                        break;
                    }
                }
        }
        if (types.contains("_m_")) {
            totalPages += playerEntry.mutes.size();
            if (currentPage != -999)
                for (BanEntry entry : playerEntry.mutes) {
                    if (++currentPage == page) {
                        playerEntry.checkBan(entry, now);
                        container.add(TextDisplay.of("### Mute"));
                        addDuration(container, entry);
                        addStamp(container, entry.banSource);
                        addRevoked(container, entry.revokedSource);
                        currentPage = -999;
                        break;
                    }
                }
        }
        if (types.contains("_sb_")) {
            totalPages += playerEntry.skinBans.size();
            if (currentPage != -999)
                for (SkinBanEntry entry : playerEntry.skinBans) {
                    if (++currentPage == page) {
                        container.add(TextDisplay.of("### Skin Ban"));
                        ProhibitorModule.generateSkinRender(entry.skin);
                        container.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(ProhibitorModule.getSkinRenderPath(entry.skin))).withDescription(entry.skin)));
                        addStamp(container, entry.banSource);
                        currentPage = -999;
                        break;
                    }
                }
        }
        if (types.contains("_w_")) {
            totalPages += playerEntry.warns.size();
            if (currentPage != -999)
                for (StampEntry entry : playerEntry.warns) {
                    if (++currentPage == page) {
                        container.add(TextDisplay.of("### Warn"));
                        addStamp(container, entry);
                        currentPage = -999;
                        break;
                    }
                }
        }
        if (types.contains("_k_")) {
            totalPages += playerEntry.kicks.size();
            if (currentPage != -999)
                for (StampEntry entry : playerEntry.kicks) {
                    if (++currentPage == page) {
                        container.add(TextDisplay.of("### Kick"));
                        addStamp(container, entry);
                        currentPage = -999;
                        break;
                    }
                }
        }

        container.add(ActionRow.of(
                Button.of(page > 0 ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY, ID + "open1," + uuid + "," + types + "," + (page - (page > 0 ? 1 : 0)), Emoji.fromUnicode("⬅️")),
                Button.secondary(ID + "open2," + uuid + "," + types + "," + (page), (page + 1) + "/" + totalPages),
                Button.of(page + 1 < totalPages ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY, ID + "open3," + uuid + "," + types + "," + (page + (page + 1 < totalPages ? 1 : 0)), Emoji.fromUnicode("➡️"))
        ));

        return Container.of(container);
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
}

package net.kyrptonaught.LEMBackend.prohibitor;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.*;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.imports.BanListImportCommand;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.imports.LinksImportCommand;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.imports.SusImportCommand;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.imports.WhitelistImportCommand;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;

import java.util.List;

public class ProhibitorDiscordCommands {

    public static List<CommandData> register() {
        return List.of(Commands.slash("prohibitor", "Punish Players").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                        .addSubcommands(
                                new SubcommandData("punish", "Issue a Punishment"),
                                new SubcommandData("view", "View Previous Punishments"),
                                new SubcommandData("pardon", "Pardon a Player"),
                                new SubcommandData("chatdisabler", "Temporarily Disable Chat"),
                                new SubcommandData("linkingdisabler", "Temporarily Disable Linking"),
                                new SubcommandData("importwhitelist", "Updates Prohibitor with a whitelist file"),
                                new SubcommandData("importbanlist", "Updates Prohibitor with a Ban List file"),
                                new SubcommandData("importlinks", "Updates Prohibitor with a Discord Links file"),
                                new SubcommandData("importsus", "Updates Prohibitor with a Sus file"),
                                new SubcommandData("personatus", "Personatus")
                        ),
                Commands.message("Prohibitor punish").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),
                Commands.message("Prohibitor view").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
        );
    }

    public static void execute(SlashCommandInteraction event) {
        if (event.getSubcommandName().equals("punish")) PunishCommand.execute(event);
        if (event.getSubcommandName().equals("view")) ViewCommand.execute(event);
        if (event.getSubcommandName().equals("pardon")) PardonCommand.execute(event);
        if (event.getSubcommandName().equals("chatdisabler")) ChatDisablerCommand.execute(event);
        if (event.getSubcommandName().equals("linkingdisabler")) LinkingDisableCommand.execute(event);
        if (event.getSubcommandName().equals("importwhitelist")) WhitelistImportCommand.execute(event);
        if (event.getSubcommandName().equals("importbanlist")) BanListImportCommand.execute(event);
        if (event.getSubcommandName().equals("personatus")) PersonatusCommand.execute(event);
        if (event.getSubcommandName().equals("importlinks")) LinksImportCommand.execute(event);
        if (event.getSubcommandName().equals("importsus")) SusImportCommand.execute(event);
    }

    public static void selectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponent().getCustomId();
        if (id.startsWith("prohibitor_punish:")) PunishCommand.selectInteraction(event);
        if (id.startsWith("prohibitor_view:")) ViewCommand.selectInteraction(event);
        if (id.startsWith("prohibitor_pardon:")) PardonCommand.selectInteraction(event);
    }

    public static void buttonInteraction(ButtonInteractionEvent event) {
        String id = event.getButton().getCustomId();
        if (id.startsWith("prohibitor_punish:")) PunishCommand.buttonInteraction(event);
        if (id.startsWith("prohibitor_view:")) ViewCommand.buttonInteraction(event);
        if (id.startsWith("prohibitor_pardon:")) PardonCommand.buttonInteraction(event);
        if (id.startsWith(PersonatusCommand.ID)) PersonatusCommand.buttonInteraction(event);
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        String id = event.getCustomId();
        if (id.startsWith("prohibitor_punish:")) PunishCommand.modalSubmit(event);
        if (id.startsWith("prohibitor_view:")) ViewCommand.modalSubmit(event);
        if (id.startsWith("prohibitor_pardon:")) PardonCommand.modalSubmit(event);
        if (id.startsWith(ChatDisablerCommand.ID)) ChatDisablerCommand.modalSubmit(event);
        if (id.startsWith(LinkingDisableCommand.ID)) LinkingDisableCommand.modalSubmit(event);
        if (id.startsWith(WhitelistImportCommand.ID)) WhitelistImportCommand.modalSubmit(event);
        if (id.startsWith(BanListImportCommand.ID)) BanListImportCommand.modalSubmit(event);
        if (id.startsWith(PersonatusCommand.ID)) PersonatusCommand.modalSubmit(event);
        if (id.startsWith(LinksImportCommand.ID)) LinksImportCommand.modalSubmit(event);
        if (id.startsWith(SusImportCommand.ID)) SusImportCommand.modalSubmit(event);
    }

    public static void messageContextInteraction(MessageContextInteractionEvent event) {
        if (event.getCommandString().contains("view")) {
            if (event.getTarget().isWebhookMessage()) {
                String name = event.getTarget().getAuthor().getName();
                String uuid = ProhibitorModule.getUUIDFromName(name);
                event.replyComponents(ViewCommand.buildHistory(uuid, 0)).useComponentsV2().setEphemeral(true).queue();
            } else {
                long name = event.getTarget().getAuthor().getIdLong();
                String uuid = LinkingManager.getMCFromDiscord(name);
                event.replyComponents(ViewCommand.buildHistory(uuid, 0)).useComponentsV2().setEphemeral(true).queue();
            }
        }
        if (event.getCommandString().contains("punish")) {
            if (event.getTarget().isWebhookMessage()) {
                String name = event.getTarget().getAuthor().getName();
                event.replyModal(PunishCommand.buildActionModal("player_name", name)).queue();
            } else {
                String name = event.getTarget().getAuthor().getId();
                event.replyModal(PunishCommand.buildActionModal("player_discord", name)).queue();
            }
        }
    }

    public static Section getTitle(String subtitle) {
        return Section.of(Thumbnail.fromUrl("https://raw.githubusercontent.com/kyrptonaught/Minigame-Resources/refs/heads/2.0/mace.png"), TextDisplay.of("# Prohibitor"), TextDisplay.of(subtitle));
    }
}
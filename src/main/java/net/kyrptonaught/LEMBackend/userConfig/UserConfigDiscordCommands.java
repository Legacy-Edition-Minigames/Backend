package net.kyrptonaught.LEMBackend.userConfig;

import com.mojang.util.UndashedUuid;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.kyrptonaught.LEMBackend.userConfig.discordCommands.*;

import java.util.List;

public class UserConfigDiscordCommands {
    public static List<CommandData> register() {
        return List.of(
                Commands.slash("userconfig", "User Config").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                        .addSubcommands(
                                new SubcommandData("view", "View a Player's Configs"),
                                new SubcommandData("set", "Set a Player's Config")
                        ),
                Commands.message("UserConfig view").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),
                Commands.message("UserConfig set").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),
                Commands.slash("legacyuserconfig", "User Config (Legacy)").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                        .addSubcommands(
                                new SubcommandData("view", "View a Player's Configs (Legacy)"),
                                new SubcommandData("set", "Set a Player's Config (Legacy)"),
                                new SubcommandData("import", "Import Advancements.zip")
                        )
        );
    }

    public static void execute(SlashCommandInteraction event) {
        if (event.getCommandString().contains("legacy") && event.getSubcommandName().equals("view")) LegacyViewCommand.execute(event);
        else if (event.getCommandString().contains("legacy") && event.getSubcommandName().equals("set")) LegacySetCommand.execute(event);
        else if (event.getSubcommandName().equals("view")) ViewCommand.execute(event);
        else if (event.getSubcommandName().equals("set")) SetCommand.execute(event);
        else if (event.getSubcommandName().equals("import")) LegacyImportAdvancementCommand.execute(event);
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        String id = event.getCustomId();
        if (id.startsWith(ViewCommand.ID)) ViewCommand.modalSubmit(event);
        else if (id.startsWith(SetCommand.ID)) SetCommand.modalSubmit(event);
        else if (id.startsWith(LegacyViewCommand.ID)) LegacyViewCommand.modalSubmit(event);
        else if (id.startsWith(LegacySetCommand.ID)) LegacySetCommand.modalSubmit(event);
        else if (id.startsWith(LegacyImportAdvancementCommand.ID)) LegacyImportAdvancementCommand.modalSubmit(event);
    }

    public static void messageContextInteraction(MessageContextInteractionEvent event) {
        if (event.getCommandString().contains("view")) {
            event.deferReply(true).queue();
            if (event.getTarget().isWebhookMessage()) {
                String name = event.getTarget().getAuthor().getName();
                String uuid = UndashedUuid.fromString(ProhibitorModule.getUUIDFromName(name)).toString();
                event.getHook().sendMessageComponents(ViewCommand.viewPlayer(name, uuid)).useComponentsV2().queue();
            } else {
                String name = event.getTarget().getAuthor().getName();
                String uuid = LinkingManager.getMCFromDiscord(event.getTarget().getAuthor().getIdLong());
                event.getHook().sendMessageComponents(ViewCommand.viewPlayer(name, uuid)).useComponentsV2().queue();
            }
        } else if (event.getCommandString().contains("set")) {
            if (event.getTarget().isWebhookMessage()) {
                String name = event.getTarget().getAuthor().getName();
                event.replyModal(SetCommand.buildModal("player_name", name)).queue();
            } else {
                String name = event.getTarget().getAuthor().getId();
                event.replyModal(SetCommand.buildModal("player_discord", name)).queue();
            }
        }
    }
}

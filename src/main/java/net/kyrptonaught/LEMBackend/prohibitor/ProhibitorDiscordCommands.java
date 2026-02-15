package net.kyrptonaught.LEMBackend.prohibitor;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.PardonCommand;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.PunishCommand;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.ViewCommand;

public class ProhibitorDiscordCommands {

    public static SlashCommandData register() {
        return Commands.slash("prohibitor", "Punish Players").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                .addSubcommands(
                        new SubcommandData("punish", "Issue a Punishment"),
                        new SubcommandData("view", "View Previous Punishments"),
                        new SubcommandData("pardon", "Pardon a Player")
                );
    }

    public static void execute(SlashCommandInteraction event) {
        if (event.getSubcommandName().equals("punish")) PunishCommand.execute(event);
        if (event.getSubcommandName().equals("view")) ViewCommand.execute(event);
        if (event.getSubcommandName().equals("pardon")) PardonCommand.execute(event);
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
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        String id = event.getCustomId();
        if (id.startsWith("prohibitor_punish:")) PunishCommand.modalSubmit(event);
        if (id.startsWith("prohibitor_view:")) ViewCommand.modalSubmit(event);
        if (id.startsWith("prohibitor_pardon:")) PardonCommand.modalSubmit(event);
    }

    public static Section getTitle(String subtitle) {
        return Section.of(Thumbnail.fromUrl("https://raw.githubusercontent.com/kyrptonaught/Minigame-Resources/refs/heads/2.0/mace.png"), TextDisplay.of("# Prohibitor"), TextDisplay.of(subtitle));
    }
}
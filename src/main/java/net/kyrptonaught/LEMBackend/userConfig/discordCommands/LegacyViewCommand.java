package net.kyrptonaught.LEMBackend.userConfig.discordCommands;

import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.PunishCommand;

import java.util.ArrayList;
import java.util.List;

public class LegacyViewCommand {
    public static String ID = "userconfiglegacy_view:";

    public static void execute(SlashCommandInteraction event) {
        event.replyModal(buildModal("player_name", null)).queue();
    }

    public static Modal buildModal(String type, String id) {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        if (type.contains("player_name"))
            container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(id).setPlaceholder("MC Name").setRequired(true).build()));
        else if (type.contains("player_uuid"))
            container.add(Label.of("Player's MC UUID: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(id).setPlaceholder("MC UUID").setRequired(true).build()));
        else if (type.contains("player_discord"))
            container.add(Label.of("Player's Discord: ", PunishCommand.discordSelect(id).setRequired(true).build()));
        else if (type.contains("player_ip"))
            container.add(Label.of("Player's IP: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(id).setPlaceholder("IP").setRequired(true).build()));

        return Modal.create(ID + type + "_submit", "View a Player's Configs (Legacy)").addComponents(container).build();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferReply(true).queue();
        String name = event.getValue("player_id").getAsString();
        String uuid = UndashedUuid.fromString(ProhibitorModule.getUUIDFromName(name)).toString();
        event.getHook().sendMessageComponents(viewPlayer(name, uuid)).useComponentsV2().queue();
    }

    public static Container viewPlayer(String name, String uuid) {
        JsonObject obj = LEMBackend.LegacyUserConfigModule.module.loadPlayer(uuid);

        List<ContainerChildComponent> container = new ArrayList<>();

        container.add(Section.of(Button.secondary("refresh", Emoji.fromUnicode("⌚")), TextDisplay.of("## *" + name + "*'s Legacy UserConfig")));
        container.add(TextDisplay.of("-# **UUID:** " + uuid));
        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        container.add(TextDisplay.of("```" + LEMBackend.gson.toJson(obj) + "```"));

        return Container.of(container);
    }
}

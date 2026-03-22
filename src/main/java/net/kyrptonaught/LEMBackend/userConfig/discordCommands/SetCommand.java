package net.kyrptonaught.LEMBackend.userConfig.discordCommands;

import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.PunishCommand;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;

import java.util.ArrayList;
import java.util.List;

public class SetCommand {
    public static String ID = "userconfig_set:";

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

        container.add(Label.of("Config Key: ", TextInput.create("key", TextInputStyle.SHORT).setPlaceholder("Config Key").setRequired(true).build()));
        container.add(Label.of("Config Value: ", TextInput.create("value", TextInputStyle.SHORT).setPlaceholder("Config Value").setRequired(true).build()));

        return Modal.create(ID + type + "_submit", "Set a Player's Configs").addComponents(container).build();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferReply(true).queue();
        String id = event.getCustomId();

        String name = null;
        String uuid = null;

        if (id.contains("player_uuid")) {
            uuid = event.getValue("player_id").getAsString();
            uuid = uuid.contains("-") ? uuid : UndashedUuid.fromString(uuid).toString();
            name = ProhibitorModule.getNameFromUUID(uuid);
        } else if (id.contains("player_name")) {
            name = event.getValue("player_id").getAsString();
            uuid = UndashedUuid.fromString(ProhibitorModule.getUUIDFromName(name)).toString();
        } else if (id.contains("player_discord")) {
            long discord = event.getValue("player_id").getAsLongList().getFirst();
            uuid = LinkingManager.getMCFromDiscord(discord);
            name = ProhibitorModule.getNameFromUUID(uuid);
        }

        String key = event.getValue("key").getAsString();
        String value = event.getValue("value").getAsString();

        JsonObject obj = LEMBackend.UserConfigModule.module.loadPlayer(uuid);
        obj.addProperty(key, value);
        LEMBackend.UserConfigModule.module.syncPlayer(uuid, LEMBackend.gson.toJson(obj));

        event.getHook().sendMessageComponents(ViewCommand.viewPlayer(name, uuid)).useComponentsV2().queue();
    }
}

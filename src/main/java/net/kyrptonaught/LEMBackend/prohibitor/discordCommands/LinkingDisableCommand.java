package net.kyrptonaught.LEMBackend.prohibitor.discordCommands;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeModule;

import java.util.ArrayList;
import java.util.List;

public class LinkingDisableCommand {
    public static String ID = "prohibitor_linkingdisabler:";

    public static void execute(SlashCommandInteraction event) {
        if (BridgeModule.servers.containsKey(event.getChannel().getName()))
            event.replyModal(buildSelectModal(event.getChannel().getName())).queue();
        else
            event.replyModal(buildSelectModal("")).queue();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        List<String> servers = event.getValue("server_select").getAsStringList();
        boolean enabled = event.getValue("enabled").getAsStringList().get(0).equals("enabled");
        String reason = event.getValue("reason").getAsString();

        String who = event.getMember().getEffectiveName();
        String source = event.getChannel().getName() + " (Discord)";

        JsonObject obj = new JsonObject();
        obj.addProperty("type", "linking_disabler");
        obj.addProperty("enabled", enabled);
        obj.addProperty("who", who);
        obj.addProperty("source", source);
        obj.addProperty("reason", reason);

        for (String server : servers) {
            BridgeModule.servers.get(server).send(obj);
        }

        event.getHook().editOriginal("Success!").queue();
    }

    private static Modal buildSelectModal(String server) {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        StringSelectMenu.Builder builder = StringSelectMenu.create("server_select").setMaxValues(BridgeModule.servers.size()).setRequired(true);

        for (String s : BridgeModule.servers.keySet()) builder.addOption(BridgeModule.servers.get(s).serverName, s);

        container.add(Label.of("Enabled: ", StringSelectMenu.create("enabled")
                .addOption("Enabled", "enabled")
                .addOption("Disabled", "disabled")
                .setDefaultValues("disabled")
                .setRequired(true).build()));

        container.add(Label.of("Servers: ", builder.setDefaultValues(server).build()));

        container.add(Label.of("Reason: ", TextInput.create("reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setRequired(true).build()));

        return Modal.create(ID + "_submit", "Linking Disabler").addComponents(container).build();
    }
}

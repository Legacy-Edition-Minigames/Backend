package net.kyrptonaught.LEMBackend.prohibitor.discordCommands;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeIn;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeModule;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorDiscordCommands;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PersonatusCommand {
    public static String ID = "prohibitor_personatus:";

    public static void execute(SlashCommandInteraction event) {
        event.deferReply(true).queue();
        buildMessage(event);
    }

    public static void buildMessage(IReplyCallback event) {
        String bridge = event.getChannel().getName();
        if (BridgeModule.servers.containsKey(bridge)) BridgeModule.servers.get(bridge).requestPersonatusStatusCommand(event);
        else personatusStatusRecieved(event, null, false);
    }

    public static void buttonInteraction(ButtonInteractionEvent event) {
        String mcName = getMcNameFromDiscord(event.getMember());

        if (event.getButton().getCustomId().contains("_refresh_")) {
            event.deferEdit().queue();
            buildMessage(event);
        } else if (event.getButton().getCustomId().contains("_enable_")) event.replyModal(enableDisableModal(event.getChannel().getName())).queue();
        else if (event.getButton().getCustomId().contains("_disguise_")) event.replyModal(checkDisguiseModal()).queue();
        else if (event.getButton().getCustomId().contains("_spoofed_")) event.replyModal(checkSpoofModal(mcName)).queue();
        else if (event.getButton().getCustomId().contains("_spoof_")) event.replyModal(setSpoofModal(mcName)).queue();
        else if (event.getButton().getCustomId().contains("_clear_")) event.replyModal(clearSpoofModal(mcName)).queue();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        event.deferEdit().queue();
        if (event.getCustomId().contains("_enable_")) {
            List<String> servers = event.getValue("server_select").getAsStringList();
            boolean enabled = event.getValue("enabled").getAsStringList().get(0).equals("enabled");

            String who = event.getMember().getEffectiveName();
            String source = event.getChannel().getName() + " (Discord)";

            JsonObject obj = new JsonObject();
            obj.addProperty("type", "peronatus_disabler");
            obj.addProperty("enabled", enabled);
            obj.addProperty("who", who);
            obj.addProperty("source", source);

            for (String server : servers) BridgeModule.servers.get(server).send(obj);

            buildMessage(event);
            event.getHook().sendMessage("Success!").setEphemeral(true).queue();
        } else if (event.getCustomId().contains("_disguise_")) {
            String mcName = event.getValue("player_id").getAsString();
            String result = mcName + " is not wearing a disguise";
            ConcurrentHashMap<String, String> spoofs = LEMBackend.KeyValueModule.module.getIdStorage("personatus");
            for (String s : spoofs.keySet()) {
                if (spoofs.get(s).equals(mcName)) {
                    result = mcName + " is actually " + s;
                    break;
                }
            }
            event.getHook().sendMessage(result).setEphemeral(true).queue();
        } else if (event.getCustomId().contains("_spoofed_")) {
            String mcName = event.getValue("player_id").getAsString();
            String result = LEMBackend.KeyValueModule.module.getValue("personatus", mcName);

            if (result != null) event.getHook().sendMessage(mcName + " is being spoofed as " + result).setEphemeral(true).queue();
            else event.getHook().sendMessage(mcName + " is not spoofing").setEphemeral(true).queue();
        } else if (event.getCustomId().contains("_spoof_")) {
            String mcName = event.getValue("player_id").getAsString();
            String spoofName = event.getValue("spoof").getAsString();
            LEMBackend.KeyValueModule.module.setValue("personatus", mcName, spoofName);

            String result = LEMBackend.KeyValueModule.module.getValue("personatus", mcName);

            buildMessage(event);
            if (result != null) event.getHook().sendMessage(mcName + " is being spoofed as " + result).setEphemeral(true).queue();
            else event.getHook().sendMessage(mcName + " is not spoofing").setEphemeral(true).queue();
            BridgeIn.sendLogMessage(event.getChannel().getName() + " (Discord)", event.getMember().getNickname() + " set " + mcName + "'s personatus to " + spoofName);
        } else if (event.getCustomId().contains("_clear_")) {
            String mcName = event.getValue("player_id").getAsString();
            LEMBackend.KeyValueModule.module.resetValue("personatus", mcName);

            String result = LEMBackend.KeyValueModule.module.getValue("personatus", mcName);

            buildMessage(event);
            if (result != null) event.getHook().sendMessage(mcName + " is being spoofed as " + result).setEphemeral(true).queue();
            else event.getHook().sendMessage(mcName + " is not spoofing").setEphemeral(true).queue();
            BridgeIn.sendLogMessage(event.getChannel().getName() + " (Discord)", event.getMember().getNickname() + " reset " + mcName + "'s personatus");
        }
    }

    public static void personatusStatusRecieved(IReplyCallback event, String server, boolean status) {
        List<ContainerChildComponent> container = new ArrayList<>();

        container.add(ProhibitorDiscordCommands.getTitle("Personatus Controls"));
        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        if (server != null) {
            container.add(TextDisplay.of("Detected Server: " + server));
            container.add(TextDisplay.of("Personatus Status: " + (status ? "Enabled" : "Disabled")));
            container.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        String mcName = getMcNameFromDiscord(event.getMember());
        if (mcName != null) {
            String response = LEMBackend.KeyValueModule.module.getValue("personatus", mcName);
            container.add(TextDisplay.of("Detected MC User: " + mcName));
            container.add(TextDisplay.of("Spoof Status: " + (response != null ? " Spoofed as " + response : "No Spoof")));
            container.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        container.add(ActionRow.of(Button.primary(ID + "button_enable_", "Enable/Disable")));
        container.add(Separator.createInvisible(Separator.Spacing.SMALL));
        container.add(ActionRow.of(Button.primary(ID + "button_disguise_", "Check Disguised"), Button.primary(ID + "button_spoofed_", "Check Spoof")));
        container.add(Separator.createInvisible(Separator.Spacing.SMALL));
        container.add(ActionRow.of(Button.primary(ID + "button_spoof_", "Set Spoof"), Button.primary(ID + "button_clear_", "Clear Spoof")));
        container.add(Separator.createInvisible(Separator.Spacing.LARGE));
        container.add(ActionRow.of(Button.primary(ID + "button_refresh_", "Refresh")));

        event.getHook().editOriginalComponents(Container.of(container)).useComponentsV2().queue();
    }

    private static Modal enableDisableModal(String server) {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        container.add(Label.of("Enabled: ", StringSelectMenu.create("enabled")
                .addOption("Enabled", "enabled")
                .addOption("Disabled", "disabled")
                .setDefaultValues("disabled")
                .setRequired(true).build()));

        StringSelectMenu.Builder builder = StringSelectMenu.create("server_select").setMaxValues(BridgeModule.servers.size()).setRequired(true);
        for (String s : BridgeModule.servers.keySet()) builder.addOption(BridgeModule.servers.get(s).serverName, s);
        container.add(Label.of("Servers: ", builder.setDefaultValues(server).build()));

        return Modal.create(ID + "_enable_submit", "Enable/Disable Personatus").addComponents(container).build();
    }

    private static Modal checkDisguiseModal() {
        List<ModalTopLevelComponent> container = new ArrayList<>();
        container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setPlaceholder("MC Name").setRequired(true).build()));
        return Modal.create(ID + "_disguise_submit", "Check In-Game Player's Disguise").addComponents(container).build();
    }

    private static Modal checkSpoofModal(String mc) {
        List<ModalTopLevelComponent> container = new ArrayList<>();
        container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(mc).setPlaceholder("MC Name").setRequired(true).build()));
        return Modal.create(ID + "_spoofed_submit", "Check Player's spoof").addComponents(container).build();
    }

    private static Modal setSpoofModal(String mc) {
        List<ModalTopLevelComponent> container = new ArrayList<>();
        container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(mc).setPlaceholder("MC Name").setRequired(true).build()));
        container.add(Label.of("Spoofed Name: ", TextInput.create("spoof", TextInputStyle.SHORT).setPlaceholder("MC Name").setRequired(true).build()));
        return Modal.create(ID + "_spoof_submit", "Set Player's spoof").addComponents(container).build();
    }

    private static Modal clearSpoofModal(String mc) {
        List<ModalTopLevelComponent> container = new ArrayList<>();
        container.add(Label.of("Player's MC Name: ", TextInput.create("player_id", TextInputStyle.SHORT).setValue(mc).setPlaceholder("MC Name").setRequired(true).build()));
        return Modal.create(ID + "_clear_submit", "Clear Player's spoof").addComponents(container).build();
    }

    private static String getMcNameFromDiscord(Member member) {
        if (member != null) {
            String mcUUID = LinkingManager.getMCFromDiscord(member.getIdLong());
            if (mcUUID != null) return ProhibitorModule.getNameFromUUID(mcUUID);
        }
        return null;
    }
}

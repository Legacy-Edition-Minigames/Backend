package net.kyrptonaught.LEMBackend.prohibitor.discordCommands;

import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorDiscordCommands;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;

import java.util.ArrayList;
import java.util.List;

public class PardonCommand {
    public static String ID = "prohibitor_pardon:";

    public static void execute(SlashCommandInteraction event) {
        event.deferReply(true).queue();
        event.getHook().sendMessageComponents(Container.of(buildMessage("player_name"))).useComponentsV2().queue();
    }

    public static void selectInteraction(StringSelectInteractionEvent event) {
        event.editComponents(Container.of(buildMessage(event.getInteraction().getValues().getFirst()))).useComponentsV2().queue();
    }

    public static void buttonInteraction(ButtonInteractionEvent event) {
        event.replyModal(buildSelectModal(event.getButton().getCustomId())).queue();
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


        String who = event.getMember().getEffectiveName() + " - Discord";
        String source = event.getGuild().getName() + " - Discord";
        String reason = event.getValue("reason").getAsString();

        ProhibitorModule.multiRevoke(types, player_uuid, who, source, reason);

        event.getHook().editOriginal("Success!").queue();
    }

    private static List<ContainerChildComponent> buildMessage(String playerLookupType) {
        List<ContainerChildComponent> container = new ArrayList<>();
        container.add(ProhibitorDiscordCommands.getTitle("Pardon a Player"));
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
        container.add(ActionRow.of(Button.primary(ID + "button_pardon_" + playerLookupType, "Pardon")));
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
                .addOption("Sussies", "_ss_")
                .setDefaultValues("_b_").setMaxValues(5)
                .setRequired(true).build()));

        container.add(Label.of("Reason: ", TextInput.create("reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setRequired(true).build()));

        return Modal.create(button + "_submit", "Pardon").addComponents(container).build();
    }
}

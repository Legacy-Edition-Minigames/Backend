package net.kyrptonaught.LEMBackend.discordBridge.linking;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeModule;
import net.kyrptonaught.LEMBackend.discordBridge.WebhookSender;
import net.kyrptonaught.LEMBackend.linking.LinkingModule;

import java.util.Collections;

public class LinkingManager {

    public static void prepareChannel(JDA jda, long channel) {
        if (channel == 0 || jda == null) return;
        MessageHistory history = MessageHistory.getHistoryFromBeginning(jda.getTextChannelById(channel)).complete();
        for (Message message : history.getRetrievedHistory()) {
            message.delete().queue();
        }
        generateDiscordInput(jda, channel);
    }

    public static void generateDiscordInput(JDA jda, long channel) {
        MessageEmbed embed = new EmbedBuilder()
                .setDescription("To link your account:\n\n1. In Minecraft join the server `legacyminigames.net`.\n2. Open the options dialog(`G` by default), select `Options` then `Discord Account Link`.\n3. Click `Link` below and enter the code from Minecraft.\n\nIf you are unable to link your account, please contact <@793437875685425154>.\nIf you are coming from patreon, send Emmie a message **on Patreon**.")
                .build();

        jda.getTextChannelById(channel).sendMessageEmbeds(Collections.singleton(embed))
                .addActionRow(Button.primary("link:start", "Link"))
                .queue();
    }

    public static void displayLinkInput(ButtonInteractionEvent event) {
        TextInput input = TextInput.create("link:input", "Link Code", TextInputStyle.SHORT)
                .setRequired(true)
                .setMinLength(5)
                .setMaxLength(5)
                .build();

        event.replyModal(Modal.create("link:modal", "Enter your Link code").addActionRow(input).build()).queue();
    }

    public static void linkInputResults(ModalInteractionEvent event) {
        String linkID = event.getValue("link:input").getAsString();
        String discordID = event.getInteraction().getMember().getId();

        LinkingModule.Link link = LEMBackend.LinkingModule.module.finishLink(linkID, discordID);
        if (link == null) {
            event.reply("An error occurred. Is the code correct?").setEphemeral(true).queue();
            return;
        }

        event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(BridgeModule.config.linkRoleID)).queue();
        event.reply("Linked!").setEphemeral(true).queue();

        WebhookSender.log(BridgeModule.config.loggingWebhookURL, "Discord Account Link", "<@" + event.getMember().getId() + "> (" + event.getMember().getEffectiveName() + ") linked their account to MC -> " + link.mcUUID());

        JsonObject obj = new JsonObject();
        obj.addProperty("type", "link_success");
        obj.add("link", LEMBackend.gson.toJsonTree(link));

        JsonObject integrations = new JsonObject();
        LEMBackend.UserConfigModule.module.integrations(link.mcUUID(), integrations);
        obj.add("integrations", integrations);

        LEMBackend.BridgeModule.module.sendMessageToServer(link.server(), obj);
    }
}

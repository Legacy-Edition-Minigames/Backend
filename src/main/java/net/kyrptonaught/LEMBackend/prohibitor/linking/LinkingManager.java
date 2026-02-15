package net.kyrptonaught.LEMBackend.prohibitor.linking;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeModule;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeOut;
import net.kyrptonaught.LEMBackend.discordBridge.WebhookSender;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorExecuter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LinkingManager {
    private static final ConcurrentHashMap<Long, String> discordLinks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LinkInProgress> linksInProgress = new ConcurrentHashMap<>();

    public static void startLink(String linkID, String mcUUID, String source) {
        linksInProgress.put(linkID, new LinkInProgress(mcUUID, source));
    }

    public static LinkInProgress finishLink(String linkID, long discordID) {
        LinkInProgress link = linksInProgress.remove(linkID);

        if (link != null) {
            ProhibitorExecuter.link(link.mcUUID, discordID, link.source);
            discordLinks.put(discordID, link.mcUUID);
            LEMBackend.ProhibitorModule.module.save(LEMBackend.gson);
            return link;
        }

        return null;
    }

    public static String getMCFromDiscord(long discordID) {
        return discordLinks.get(discordID);
    }

    public static Map<Long, String> getSave() {
        return discordLinks;
    }

    public static void load(Map<Long, String> discordLinks) {
        if (discordLinks != null && !discordLinks.isEmpty())
            LinkingManager.discordLinks.putAll(discordLinks);
    }

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
                .addComponents(ActionRow.of(Button.primary("link:start", "Link")))
                .queue();
    }

    public static void displayLinkInput(ButtonInteractionEvent event) {
        TextInput input = TextInput.create("link:input", TextInputStyle.SHORT)
                .setPlaceholder("Link Code")
                .setRequired(true)
                .setMinLength(5)
                .setMaxLength(5)
                .build();

        event.replyModal(Modal.create("link:modal", "Enter your Link code")
                .addComponents(Label.of("Link Code:", input)).build()).queue();
    }

    public static void linkInputResults(ModalInteractionEvent event) {
        String linkID = event.getValue("link:input").getAsString();
        long discordID = event.getInteraction().getMember().getIdLong();

        LinkInProgress link = finishLink(linkID, discordID);
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

        BridgeOut.sendMessageToServer(link.source(), obj);
    }

    public record LinkInProgress(String mcUUID, String source) {
    }
}

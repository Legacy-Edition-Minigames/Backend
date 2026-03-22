package net.kyrptonaught.LEMBackend.discordBridge;

import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;

import java.util.EnumSet;

public class WebhookSender {
    public static void sendMessage(Webhook webhook, String name, String url, String msg) {
        sendMessage(webhook, name, url, msg, false);
    }

    public static void sendMessage(Webhook webhook, String name, String url, String msg, boolean allowMentions) {
        if (webhook != null) {
            WebhookMessageCreateAction<Message> action = webhook.sendMessage(msg).setUsername(name).setAvatarUrl(url);
            if (allowMentions) action.setAllowedMentions(EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE, Message.MentionType.EVERYONE));
            action.queue();
        }
    }

    public static void log(Webhook webhook, String logSource, String message) {
        if (webhook != null) {
            webhook.sendMessageEmbeds(new MessageEmbed(null, logSource, message, EmbedType.RICH, null, 0xa87132, null, null, null, null, null, null, null)).queue();
        }
    }

    public static void logMention(Webhook webhook, String logSource, String message, long moderatorRoleID, boolean allowMentions) {
        if (webhook != null) {
            WebhookMessageCreateAction<Message> action = webhook.sendMessage("**" + logSource + "** <@&" + moderatorRoleID + ">\n" + message);
            if (allowMentions) action.setAllowedMentions(EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE, Message.MentionType.EVERYONE));
            action.queue();
        }
    }
}

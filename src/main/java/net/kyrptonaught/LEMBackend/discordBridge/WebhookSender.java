package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.IO;

public class WebhookSender {
    public static void sendMessage(String webhook, String name, String url, String msg) {
        sendMessage(webhook, name, url, msg, false);
    }

    public static void sendMessage(String webhook, String name, String url, String msg, boolean allowMentions) {
        if (webhook != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("content", msg);
            payload.addProperty("username", name);
            payload.addProperty("avatar_url", url);

            JsonObject mentions = new JsonObject();
            if (allowMentions) {
                JsonArray parse = new JsonArray();
                parse.add("users");
                parse.add("roles");
                parse.add("everyone");
                mentions.add("parse", parse);
            } else mentions.add("parse", new JsonArray());

            payload.add("allowed_mentions", mentions);
            IO.asyncPostAlt(webhook, payload.toString());
        }
    }

    public static void log(String webhook, String logSource, String message) {
        if (webhook != null) {

            JsonObject embed = new JsonObject();
            embed.addProperty("title", logSource);
            embed.addProperty("description", message);
            embed.addProperty("color", 0xa87132);

            JsonArray embeds = new JsonArray();
            embeds.add(embed);

            JsonObject payload = new JsonObject();
            payload.add("embeds", embeds);

            IO.asyncPostAlt(webhook, payload.toString());
        }
    }

    public static void logMention(String webhook, String logSource, String message, long moderatorRoleID, boolean allowMentions) {
        if (webhook != null) {

            JsonObject payload = new JsonObject();
            payload.addProperty("content", "**" + logSource + "** <@&" + moderatorRoleID + ">\n" + message);

            JsonObject mentions = new JsonObject();
            if (allowMentions) {
                JsonArray parse = new JsonArray();
                parse.add("users");
                parse.add("roles");
                parse.add("everyone");
                mentions.add("parse", parse);
            } else mentions.add("parse", new JsonArray());

            payload.add("allowed_mentions", mentions);

            IO.asyncPostAlt(webhook, payload.toString());
        }
    }
}

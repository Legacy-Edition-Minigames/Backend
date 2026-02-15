package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.discordBridge.format.FormatToDiscord;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorExecuter;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.Collections;
import java.util.Map;

public class BridgeIn {

    public static void onMessage(String bridge, JsonObject obj) {
        JDA jda = BridgeModule.jda;
        Map<String, ServerInfo> servers = BridgeModule.servers;
        DiscordBridgeConfig config = BridgeModule.config;

        if (obj.get("type").getAsString().equals("chat")) {
            handleChatMessage(bridge, obj);
        } else if (obj.get("type").getAsString().equals("game")) {
            Text text = TextCodecs.CODEC.parse(JsonOps.INSTANCE, obj.get("msg")).result().get();
            String msg = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, text, true);
            sendEmbed(jda, servers.get(bridge).discordChannelID, null, msg, obj.get("color").getAsInt());
        } else if (obj.get("type").getAsString().equals("log")) {
            WebhookSender.log(config.loggingWebhookURL, obj.get("server_name").getAsString(), obj.get("msg").getAsString());
        } else if (obj.get("type").getAsString().equals("log_text")) {
            Text text = TextCodecs.CODEC.parse(JsonOps.INSTANCE, obj.get("msg")).result().get();
            String msg = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, text, true);
            if (obj.get("ping").getAsBoolean())
                WebhookSender.logMention(config.loggingWebhookURL, obj.get("server_name").getAsString(), msg, config.moderatorRoleID, true);
            else
                WebhookSender.log(config.loggingWebhookURL, obj.get("server_name").getAsString(), msg);
        } else if (obj.get("type").getAsString().equals("lock")) {
            BridgeActions.lockChannel(jda, servers.get(bridge).discordChannelID, config.linkRoleID, obj.get("locked").getAsBoolean());
        } else if (obj.get("type").getAsString().equals("info_reply")) {
            BotCommands.infoCommandResponse(obj, servers.get(bridge).infoCommandInteraction);
            servers.get(bridge).infoCommandInteraction = null;
        } else if (obj.get("type").getAsString().equals("game_start_info")) {
            BotCommands.gameStartInfo(jda, servers.get(bridge).discordChannelID, obj);
        }
    }

    public static void handleChatMessage(String bridge, JsonObject obj) {
        JDA jda = BridgeModule.jda;
        Map<String, ServerInfo> servers = BridgeModule.servers;
        DiscordBridgeConfig config = BridgeModule.config;

        Text text = TextCodecs.CODEC.parse(JsonOps.INSTANCE, obj.get("msg")).result().get();
        String msg = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, text, true);
        boolean isMuted = obj.get("muted").getAsBoolean();

        if (isMuted) {
            if (LEMBackend.ProhibitorModule.module.canPlayerChat(obj.get("player_uuid").getAsString())) {
                JsonObject custom = new JsonObject();
                custom.add("msg", obj.get("msg"));
                ProhibitorExecuter.notifyServer("Auto Expiration", obj.get("player_uuid").getAsString(), "unmute_w_msg", Text.translatable("mco.configure.world.subscription.expired"), custom);
                isMuted = false;
            }
        }

        if (!isMuted)
            WebhookSender.sendMessage(servers.get(bridge).discordChannelWebhook, obj.get("display_name").getAsString(), FormatToDiscord.getUserHeadURL(config.playerSkinURL, obj.get("display_name").getAsString(), obj.get("player_uuid").getAsString()), msg);
    }

    public static void sendMessage(JDA jda, long channel, String msg, boolean mentions) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent(msg)
                .setAllowedMentions(mentions ? null : Collections.emptyList())
                .build();

        jda.getTextChannelById(channel).sendMessage(message).queue();
    }

    public static void sendEmbed(JDA jda, long channel, String title, String msg, int hexColor) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        if (title != null) embedBuilder.setTitle(title);
        if (hexColor != 0) embedBuilder.setColor(hexColor);
        embedBuilder.setDescription(msg);

        jda.getTextChannelById(channel).sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static void sendLogMessage(String source, String message) {
        WebhookSender.log(BridgeModule.config.loggingWebhookURL, source, message);
    }

    public static void sendLogMessage(String source, Text message) {
        sendLogMessage(source, FormatToDiscord.toDiscord(BridgeModule.jda, LEMBackend.minecraftServer, message));
    }
}

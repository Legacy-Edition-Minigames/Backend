package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyrptonaught.LEMBackend.discordBridge.format.FormatToMC;
import net.kyrptonaught.LEMBackend.prohibitor.ChatFilter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public class BridgeOut {

    public static void onDiscordMessage(MessageReceivedEvent event) {
        if (event == null || !shouldRespondToMessage(event)) return;

        if (!ChatFilter.handleDiscordMessage(event)) return;

        if (isAllowedChannel(event.getChannel().getName(), event.getChannel().getIdLong())) {
            if (event.getMessage().getReferencedMessage() != null) {
                Component message = FormatToMC.parseMessage(event.getMessage().getReferencedMessage(), Component.literal("    ┌──── ").withStyle(ChatFormatting.GRAY), false);
                sendMessageToServer(event.getChannel().getName(), message);
            }

            Role adminMessageRole = event.getGuild().getRoleById(BridgeModule.config.adminMessageRoleID);
            boolean admin = event.getMember().getRoles().contains(adminMessageRole);

            Component message = FormatToMC.parseMessage(event.getMessage(), Component.literal("[Discord] ").withStyle(ChatFormatting.BLUE), admin);
            if (message != null)
                sendMessageToServer(event.getChannel().getName(), message);
        }
    }

    public static void sendMessageToServer(String bridge, Component message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "chat");
        encodeText(obj, "msg", message);
        sendMessageToServer(bridge, obj);
    }

    public static void sendMessageToServer(String bridge, JsonObject obj) {
        BridgeModule.servers.get(bridge).send(obj);
    }

    public static void sendMessageToAllServers(String type, JsonObject obj) {
        obj.addProperty("type", type);
        for (ServerInfo info : BridgeModule.servers.values()) info.send(obj);
    }

    private static boolean shouldRespondToMessage(MessageReceivedEvent event) {
        return (event.getMessage().getType() == MessageType.DEFAULT || event.getMessage().getType() == MessageType.INLINE_REPLY) &&
                !event.isWebhookMessage() &&
                event.getAuthor().getIdLong() != BridgeModule.jda.getSelfUser().getIdLong();
    }

    private static boolean isAllowedChannel(String channel, long channelID) {
        return BridgeModule.servers.containsKey(channel) && BridgeModule.servers.get(channel).chatChannelID == channelID;
    }

    public static void encodeText(JsonObject obj, String name, Component text) {
        obj.add(name, ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow());
    }

    public static JsonElement encodeText(Component text) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow();
    }
}
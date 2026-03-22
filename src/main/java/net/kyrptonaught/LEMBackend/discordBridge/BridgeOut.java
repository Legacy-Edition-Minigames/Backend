package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyrptonaught.LEMBackend.discordBridge.format.FormatToMC;
import net.kyrptonaught.LEMBackend.prohibitor.ChatFilter;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;

public class BridgeOut {

    public static void onDiscordMessage(MessageReceivedEvent event) {
        if (event == null || !shouldRespondToMessage(event)) return;

        if (!ChatFilter.handleDiscordMessage(event)) return;

        if (isAllowedChannel(event.getChannel().getName(), event.getChannel().getIdLong())) {
            if (event.getMessage().getReferencedMessage() != null) {
                Text message = FormatToMC.parseMessage(event.getMessage().getReferencedMessage(), Text.literal("    ┌──── ").formatted(Formatting.GRAY), false);
                sendMessageToServer(event.getChannel().getName(), message);
            }

            Role adminMessageRole = event.getGuild().getRoleById(BridgeModule.config.adminMessageRoleID);
            boolean admin = event.getMember().getRoles().contains(adminMessageRole);

            Text message = FormatToMC.parseMessage(event.getMessage(), Text.literal("[Discord] ").formatted(Formatting.BLUE), admin);
            if (message != null)
                sendMessageToServer(event.getChannel().getName(), message);
        }
    }

    public static void sendMessageToServer(String bridge, Text message) {
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

    public static void encodeText(JsonObject obj, String name, Text text) {
        obj.add(name, TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow());
    }
}

package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.javalin.websocket.WsContext;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;
import net.kyrptonaught.LEMBackend.discordBridge.format.FormatToDiscord;
import net.kyrptonaught.LEMBackend.discordBridge.format.FormatToMC;
import net.kyrptonaught.LEMBackend.discordBridge.linking.LinkingManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BridgeModule extends Module {
    public static DiscordBridgeConfig config;
    public static final Map<String, ServerInfo> servers = new ConcurrentHashMap<>();
    private JDA jda;

    public BridgeModule() {
        super("discordBridge");
    }

    @Override
    public void load(Gson gson) {
        createDirectories();
        config = readFileJson(gson, "discordbridge.json", DiscordBridgeConfig.class);

        buildBot();
    }

    @Override
    public void save(Gson gson) {
        createDirectories();
        if (config != null)
            writeFile("discordbridge.json", LEMBackend.gson.toJson(config));
        else
            writeFile("discordbridge.json", LEMBackend.gson.toJson(new DiscordBridgeConfig()));

        jda.shutdown();
    }

    public void registerServer(String bridge, WsContext ctx) {
        TextChannel channel = BridgeActions.getOrCreateChannel(jda, config.bridgeCategoryID, bridge);
        Webhook webhook = BridgeActions.getOrCreateWebhook(jda, channel, "Heirloom");

        servers.put(bridge, new ServerInfo(ctx, channel.getIdLong(), webhook.getUrl()));
    }

    public void buildBot() {
        if (config.botToken == null)
            return;

        jda = BridgeActions.create(config.botToken, config.playingStatus);
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                onDiscordMessage(event);
            }

            @Override
            public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
                BotCommands.execute(jda, event);
            }

            @Override
            public void onModalInteraction(@NotNull ModalInteractionEvent event) {
                BotCommands.modalInteraction(jda, event);
            }

            @Override
            public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
                BotCommands.buttonPressed(jda, event);
            }
        });
        BotCommands.registerCommands(jda);
        LinkingManager.prepareChannel(jda, config.linkChannelID);
    }


    public void onDiscordMessage(MessageReceivedEvent event) {
        if (event != null && shouldRespondToMessage(event)) {
            if (event.getMessage().getReferencedMessage() != null) {
                Text message = FormatToMC.parseMessage(event.getMessage().getReferencedMessage(), Text.literal("    ┌──── ").formatted(Formatting.GRAY), false);
                sendMessageToServer(event.getChannel().getName(), message);
            }

            Role adminMessageRole = event.getGuild().getRoleById(config.adminMessageRoleID);
            boolean admin = event.getMember().getRoles().contains(adminMessageRole);

            Text message = FormatToMC.parseMessage(event.getMessage(), Text.literal("[Discord] ").formatted(Formatting.BLUE), admin);
            if (message != null)
                sendMessageToServer(event.getChannel().getName(), message);
        }
    }

    public void sendMessageToServer(String bridge, Text message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "chat");
        encodeText(obj, "msg", message);
        sendMessageToServer(bridge, obj);
    }

    public void sendMessageToServer(String bridge, JsonObject obj) {
        servers.get(bridge).send(obj);
    }

    public void onMinecraftMessage(String bridge, JsonObject obj) {
        if (obj.get("type").getAsString().equals("chat")) {
            Text text = TextCodecs.CODEC.parse(JsonOps.INSTANCE, obj.get("msg")).result().get();
            String msg = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, text, true);
            WebhookSender.sendMessage(servers.get(bridge).discordChannelWebhook, obj.get("display_name").getAsString(), FormatToDiscord.getUserHeadURL(config.playerSkinURL, obj.get("display_name").getAsString(), obj.get("display_name").getAsString()), msg);
        } else if (obj.get("type").getAsString().equals("game")) {
            Text text = TextCodecs.CODEC.parse(JsonOps.INSTANCE, obj.get("msg")).result().get();
            String msg = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, text, true);
            BridgeActions.sendEmbed(jda, servers.get(bridge).discordChannelID, null, msg, obj.get("color").getAsInt());
        } else if (obj.get("type").getAsString().equals("log")) {
            WebhookSender.log(config.loggingWebhookURL, obj.get("server_name").getAsString(), obj.get("msg").getAsString());
        } else if (obj.get("type").getAsString().equals("log_text")) {
            Text text = TextCodecs.CODEC.parse(JsonOps.INSTANCE, obj.get("msg")).result().get();
            String msg = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, text, true);
            WebhookSender.logMention(config.loggingWebhookURL, obj.get("server_name").getAsString(), msg, config.moderatorRoleID, true);
        } else if (obj.get("type").getAsString().equals("lock")) {
            BridgeActions.lockChannel(jda, servers.get(bridge).discordChannelID, config.linkRoleID, obj.get("locked").getAsBoolean());
        } else if (obj.get("type").getAsString().equals("info_reply")) {
            BotCommands.infoCommandResponse(obj, servers.get(bridge).infoCommandInteraction);
            servers.get(bridge).infoCommandInteraction = null;
        }
    }

    public PatreonTier getPatreonTier(String discordID) {
        Guild guild = jda.getGuildById(config.discordServerID);
        if (guild != null) {
            Member member = guild.getMemberById(discordID);

            if (hasRole(member, 935005241458454538L)) return PatreonTier.TINY;
            if (hasRole(member, 935005356558524461L)) return PatreonTier.SMALL;
            if (hasRole(member, 935012217953345607L)) return PatreonTier.STANDARD;
            if (hasRole(member, 935005435579236374L)) return PatreonTier.LARGE;
            if (hasRole(member, 935005499861110825L)) return PatreonTier.LARGEPLUS;
            if (hasRole(member, 1017354249404940319L)) return PatreonTier.PREVIOUS;

            if (hasRole(member, 1090037470911008848L)) return PatreonTier.LARGE;//dev server
        }

        return PatreonTier.NONE;
    }

    private boolean hasRole(Member member, long roleID) {
        if (member == null) return false;
        Role role = member.getGuild().getRoleById(roleID);
        if (role == null) return false;
        return member.getUnsortedRoles().contains(role);
    }

    private boolean shouldRespondToMessage(MessageReceivedEvent event) {
        return (event.getMessage().getType() == MessageType.DEFAULT || event.getMessage().getType() == MessageType.INLINE_REPLY) &&
                !event.isWebhookMessage() &&
                event.getAuthor().getIdLong() != jda.getSelfUser().getIdLong() &&
                (isAllowedChannel(event.getChannel().getName()));
    }

    public boolean isAllowedChannel(String channel) {
        return servers.containsKey(channel);
    }

    private static void encodeText(JsonObject obj, String name, Text text) {
        obj.add(name, TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow());
    }
}

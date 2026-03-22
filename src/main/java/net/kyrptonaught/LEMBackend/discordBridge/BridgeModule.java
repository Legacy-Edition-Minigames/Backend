package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.Gson;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsContext;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BridgeModule extends Module {
    public static DiscordBridgeConfig config;
    public static final Map<String, ServerInfo> servers = new ConcurrentHashMap<>();
    public static JDA jda;
    public static Webhook adminLogWebhook;
    public static Webhook logWebhook;

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
        TextChannel chatChannel = BridgeActions.getOrCreateChannel(jda, config.bridgeCategoryID, bridge);
        Webhook chatWebhook = BridgeActions.getOrCreateWebhook(jda, chatChannel, "Heirloom");

        TextChannel logChannel = BridgeActions.getOrCreateChannel(jda, config.logCategoryID, bridge);
        Webhook logWebhook = BridgeActions.getOrCreateWebhook(jda, logChannel, "Heirloom");

        servers.put(bridge, new ServerInfo(ctx, chatChannel.getIdLong(), chatWebhook, logChannel.getIdLong(), logWebhook));
    }

    public void removeServer(WsCloseContext ctx) {
        servers.values().removeIf(serverInfo -> serverInfo.socketConnection.sessionId().equals(ctx.sessionId()));
    }

    public void buildBot() {
        if (config.botToken == null)
            return;

        jda = BridgeActions.create(config.botToken, config.playingStatus);
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(MessageReceivedEvent event) {
                BridgeOut.onDiscordMessage(event);
            }

            @Override
            public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
                BotCommands.execute(jda, event);
            }

            @Override
            public void onModalInteraction(ModalInteractionEvent event) {
                BotCommands.modalInteraction(jda, event);
            }

            @Override
            public void onButtonInteraction(ButtonInteractionEvent event) {
                BotCommands.buttonPressed(jda, event);
            }

            @Override
            public void onMessageContextInteraction(MessageContextInteractionEvent event) {
                BotCommands.messageContextInteraction(jda, event);
            }

            @Override
            public void onStringSelectInteraction(StringSelectInteractionEvent event) {
                BotCommands.selectInteraction(jda, event);
            }

        });
        BotCommands.registerCommands(jda);
        LinkingManager.prepareChannel(jda, config.linkChannelID);
        adminLogWebhook = BridgeActions.getOrCreateWebhook(jda, jda.getTextChannelById(config.loggingChannelID), "Heirloom");
        logWebhook = BridgeActions.getOrCreateWebhook(jda, BridgeActions.getOrCreateChannel(jda, config.logCategoryID, "backend"), "Heirloom");

        Logger rootLogger = (Logger) LogManager.getRootLogger();
        BridgeLogger appender = new BridgeLogger("Bridge Logger", null, null);
        appender.start();
        rootLogger.addAppender(appender);
    }


    public PatreonTier getPatreonTier(long discordID) {
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

    public String createPlayerReport(String reportedPlayer, String content, long reportingPlayer) {
        ForumPost post = BridgeActions.createForumPost(jda, 1474598684137095210L, reportedPlayer, MessageCreateData.fromContent(content));
        post.getThreadChannel().addThreadMemberById(reportingPlayer).queue();
        post.getThreadChannel().sendMessage("Ping as requested: <@" + reportingPlayer + ">").mentionUsers(reportingPlayer).queue();
        return post.getMessage().getJumpUrl();
    }
}

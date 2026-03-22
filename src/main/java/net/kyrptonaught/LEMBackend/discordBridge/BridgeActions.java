package net.kyrptonaught.LEMBackend.discordBridge;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class BridgeActions {

    public static JDA create(String botToken, String status) {
        try {
            return JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .enableIntents(GatewayIntent.GUILD_EXPRESSIONS)
                    .enableCache(CacheFlag.EMOJI)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setActivity(Activity.playing(status))
                    .build().awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static TextChannel getOrCreateChannel(JDA jda, long bridgeCategoryID, String name) {
        try {
            Category category = jda.getCategoryById(bridgeCategoryID);
            for (TextChannel channel : category.getTextChannels())
                if (channel.getName().equals(name)) return channel;

            return category.createTextChannel(name).submit().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Webhook getOrCreateWebhook(JDA jda, TextChannel channel, String name) {
        try {
            for (Webhook webhook : channel.retrieveWebhooks().submit().get())
                if (webhook.getName().equalsIgnoreCase(name)) return webhook;

            return channel.createWebhook(name).submit().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void lockChannel(JDA jda, long channel, long linkRoleID, boolean locked) {
        if (locked) {
            jda.getTextChannelById(channel).upsertPermissionOverride(jda.getRoleById(linkRoleID))
                    .deny(Permission.MESSAGE_SEND).queue();
        } else {
            jda.getTextChannelById(channel).upsertPermissionOverride(jda.getRoleById(linkRoleID))
                    .grant(Permission.MESSAGE_SEND).queue();
        }
    }

    public static ForumPost createForumPost(JDA jda, long forumID, String name, MessageCreateData msg) {
        try {
            ForumChannel category = jda.getForumChannelById(forumID);
            return category.createForumPost(name, msg).submit().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

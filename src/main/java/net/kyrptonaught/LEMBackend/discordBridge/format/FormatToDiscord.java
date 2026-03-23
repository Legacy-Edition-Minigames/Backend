package net.kyrptonaught.LEMBackend.discordBridge.format;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

import java.util.Optional;

public class FormatToDiscord {
    public static String toDiscord(JDA jda, MinecraftServer server, String text) {
        return toDiscord(jda, server, text, false);
    }

    public static String toDiscord(JDA jda, MinecraftServer server, String text, boolean escapeFormat) {
        if (escapeFormat) text = escapeFormatting(text);

        for (RichCustomEmoji emoji : jda.getEmojiCache())
            text = text.replaceAll(":" + emoji.getName() + ":", emoji.getAsMention());
        return text;
    }

    public static String toDiscord(JDA jda, MinecraftServer server, Text text) {
        return toDiscord(jda, server, text, false);
    }

    public static String toDiscord(JDA jda, MinecraftServer server, Text text, boolean escapeFormat) {
        try {
            text = Texts.parse(server.getCommandSource(), text, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuilder output = new StringBuilder();
        text.visit((style, text2) -> {
            if (escapeFormat) text2 = escapeFormatting(text2);

            StringBuilder modifier = new StringBuilder();
            if (style.isUnderlined()) modifier.append("__");
            if (style.isStrikethrough()) modifier.append("~~");
            if (style.isItalic()) modifier.append("*");
            if (style.isBold()) modifier.append("**");

            text2 = text2.replaceAll("[\\uF801-\\uF880]", "");

            output.append(modifier).append(text2).append(modifier.reverse());
            return Optional.empty();
        }, text.getStyle());

        return toDiscord(jda, server, output.toString());
    }

    public static String escapeFormatting(String word) {
        return word
                .replaceAll("_", "\\\\_")
                .replaceAll("\\*", "\\\\*")
                .replaceAll("~", "\\\\~")
                .replaceAll("`", "\\\\`")
                .replaceAll(">", "\\\\>");
    }

    public static String getUserHeadURL(String url, String name, String uuid) {
        return url
                .replace("%PLAYERNAME%", name)
                .replace("%PLAYERUUID%", uuid);
    }
}
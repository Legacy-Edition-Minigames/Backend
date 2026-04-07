package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.gson.JsonArray;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeModule;
import net.kyrptonaught.LEMBackend.discordBridge.WebhookSender;
import net.kyrptonaught.LEMBackend.prohibitor.actions.MuteAction;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class ChatFilter {
    private static final Set<String> blocklist = new HashSet<>();

    public static boolean scanMessage(String message) {
        String[] words = message.split(" ");

        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            String word2 = word.charAt(word.length() - 1) == 's' ? word.substring(0, word.length() - 1).toLowerCase() : word;

            if (blocklist.contains(word) || blocklist.contains(word2)) return false;

            if (i > 0) {
                String prev = words[i - 1].toLowerCase();
                String prev2 = prev.charAt(prev.length() - 1) == 's' ? prev.substring(0, prev.length() - 1).toLowerCase() : prev;
                if (blocklist.contains(prev + word) || blocklist.contains(prev + word2) || blocklist.contains(prev2 + word) || blocklist.contains(prev2 + word2)) return false;
            }
        }
        return true;
    }

    public static void handleChatMessage(String msg, String name, String uuid, String bridge) {
        msg = "<" + name + "> ||" + msg + "||";
        WebhookSender.logMention(BridgeModule.adminLogWebhook, "Inappropriate Message", msg, BridgeModule.config.moderatorRoleID, true);
        MuteAction.tempMute(uuid, "Chat Filter", bridge + " - Server", "Inappropriate Message: " + msg, 5, (byte) ChronoUnit.MINUTES.ordinal());
    }

    public static boolean handleDiscordMessage(MessageReceivedEvent event) {
        if (!ChatFilter.scanMessage(event.getMessage().getContentDisplay())) {
            String msg = "<@" + event.getMember().getId() + "> (" + event.getMember().getEffectiveName() + ") ||" + event.getMessage().getContentDisplay() + "||";
            WebhookSender.logMention(BridgeModule.adminLogWebhook, "Inappropriate Message", msg, BridgeModule.config.moderatorRoleID, true);

            event.getMessage().delete().reason("Inappropriate Message").queue();
            return false;
        }
        return true;
    }

    public static JsonArray getBlocklist() {
        JsonArray arr = new JsonArray(blocklist.size());
        for (String word : blocklist) arr.add(word);
        return arr;
    }

    public static void genWords() {
        blocklist.add("fag");
        blocklist.add("faggot");
        blocklist.add("f@g");
        blocklist.add("f@ggot");
        blocklist.add("faqqot");

        blocklist.add("nig");
        blocklist.add("nibba");
        blocklist.add("nigga");
        blocklist.add("nigger");
        blocklist.add("nibber");
        blocklist.add("negger");
        blocklist.add("n1gger");
        blocklist.add("niqqa");
        blocklist.add("niqqer");
        blocklist.add("n1gga");
        blocklist.add("nigg@");
        blocklist.add("n1gga");
        blocklist.add("negga");
        blocklist.add("nigguh");
        blocklist.add("nga");

        blocklist.add("spick");
        blocklist.add("beaner");
        blocklist.add("spacker");
        blocklist.add("siegheil");
        blocklist.add("putitio");
        blocklist.add("munting");
        blocklist.add("blackpeople");
        blocklist.add("jew");
        blocklist.add("chink");
        blocklist.add("chigger");
    }
}
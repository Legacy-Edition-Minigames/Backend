package net.kyrptonaught.LEMBackend.prohibitor;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeIn;
import net.kyrptonaught.LEMBackend.prohibitor.actions.MuteAction;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class ChatFilter {
    private static final Set<String> badWords = new HashSet<>();

    public static boolean scanMessage(String message) {
        String[] words = message.split(" ");

        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            String word2 = word.charAt(word.length() - 1) == 's' ? word.substring(0, word.length() - 1).toLowerCase() : word;

            if (badWords.contains(word) || badWords.contains(word2)) return false;

            if (i > 0) {
                String prev = words[i - 1].toLowerCase();
                String prev2 = prev.charAt(prev.length() - 1) == 's' ? prev.substring(0, prev.length() - 1).toLowerCase() : prev;
                if (badWords.contains(prev + word) || badWords.contains(prev + word2) || badWords.contains(prev2 + word) || badWords.contains(prev2 + word2)) return false;
            }
        }
        return true;
    }


    public static boolean handleChatMessage(String msg, String name, String uuid, String bridge) {
        if (!ChatFilter.scanMessage(msg)) {
            BridgeIn.sendLogMessage("Inappropriate Message", "<" + name + "> ||" + msg + "||");
            MuteAction.tempMute(uuid, "Chat Filter", bridge + " - Server", "Inappropriate Message: " + msg, 5, (byte) ChronoUnit.MINUTES.ordinal());
            return false;
        }
        return true;
    }

    public static boolean handleDiscordMessage(MessageReceivedEvent event) {
        if (!ChatFilter.scanMessage(event.getMessage().getContentDisplay())) {
            BridgeIn.sendLogMessage("Inappropriate Message", "<@" + event.getMember().getId() + "> (" + event.getMember().getEffectiveName() + ") ||" + event.getMessage().getContentDisplay() + "||");
            event.getMessage().delete().reason("Inappropriate Message").queue();
            return false;
        }
        return true;
    }

    public static void genWords() {
        badWords.add("fag");
        badWords.add("faggot");
        badWords.add("f@g");
        badWords.add("f@ggot");
        badWords.add("faqqot");

        badWords.add("nig");
        badWords.add("nibba");
        badWords.add("nigga");
        badWords.add("nigger");
        badWords.add("nibber");
        badWords.add("negger");
        badWords.add("n1gger");
        badWords.add("niqqa");
        badWords.add("niqqer");
        badWords.add("n1gga");
        badWords.add("nigg@");
        badWords.add("n1gga");
        badWords.add("negga");
        badWords.add("nigguh");
        badWords.add("nga");

        badWords.add("spick");
        badWords.add("beaner");
        badWords.add("spacker");
        badWords.add("siegheil");
        badWords.add("putitio");
        badWords.add("munting");
        badWords.add("blackpeople");
        badWords.add("jew");
        badWords.add("chink");
        badWords.add("chigger");
    }
}

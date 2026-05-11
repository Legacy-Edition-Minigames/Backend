package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyrptonaught.LEMBackend.JsonHelper;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeModule;
import net.kyrptonaught.LEMBackend.discordBridge.WebhookSender;
import net.kyrptonaught.LEMBackend.prohibitor.actions.MuteAction;

import java.text.Normalizer;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ChatFilter {
    private static final Set<String> blacklist2 = new HashSet<>();
    private static final Set<String> whitelist2 = new HashSet<>();
    private static final Map<Character, Character> leetMap = new HashMap<>();

    public static Set<String> blacklist() {
        return Configs.chatBlacklist;
    }

    public static Set<String> whitelist() {
        return Configs.chatWhitelist;
    }

    public static void load() {
        blacklist2.clear();
        whitelist2.clear();
        leetMap.clear();

        leetMap.put('@', 'a');
        leetMap.put('4', 'a');
        leetMap.put('1', 'i');
        leetMap.put('!', 'i');
        leetMap.put('3', 'e');
        leetMap.put('0', 'o');
        leetMap.put('$', 's');
        leetMap.put('5', 's');

        for (String word : blacklist()) {
            StringBuilder builder = new StringBuilder();
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                builder.append(chars[i]);
                if (i != chars.length - 1) builder.append(" ");
            }
            blacklist2.add(builder.toString());
        }

        for (String word : whitelist()) {
            StringBuilder builder = new StringBuilder();
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                builder.append(chars[i]);
                if (i != chars.length - 1) builder.append(" ");
            }
            whitelist2.add(builder.toString());
        }
        System.out.println("Loaded ChatFilter");
    }

    public static void handleChatMessage(String msg, String name, String uuid, String bridge) {
        msg = "<" + name + "> ||" + msg + "||";
        WebhookSender.logMention(BridgeModule.adminLogWebhook, "Inappropriate Message", msg, BridgeModule.config.moderatorRoleID, true);
        MuteAction.tempMute(uuid, "Chat Filter", bridge + " - Server", "Inappropriate Message: " + msg, 5, (byte) ChronoUnit.MINUTES.ordinal());
    }

    public static boolean handleDiscordMessage(MessageReceivedEvent event) {
        if (ChatFilter.containsProfanity(event.getMessage().getContentDisplay())) {
            String msg = "<@" + event.getMember().getId() + "> (" + event.getMember().getEffectiveName() + ") ||" + event.getMessage().getContentDisplay() + "||";
            WebhookSender.logMention(BridgeModule.adminLogWebhook, "Inappropriate Message", msg, BridgeModule.config.moderatorRoleID, true);

            event.getMessage().delete().reason("Inappropriate Message").queue();
            return false;
        }
        return true;
    }

    public static boolean containsProfanity(String input) {
        if (input == null || input.isEmpty()) return false;

        String whole = transform(input);
        for (String s : whitelist2) {
            if (whole.contains(s)) {
                whole = null;
                break;
            }
        }
        if (whole != null)
            for (String s : blacklist2) {
                if (whole.contains(s)) {
                    return true;
                }
            }

        for (String word : input.split(" ")) {
            // Remove Other Characters
            word = transform(word).replaceAll("[^a-z]", "");

            //Whitelisted word or whitelist word non-plural
            if (whitelist().contains(word) || whitelist().contains(word.replaceAll("s$", ""))) continue;

            // Blacklisted word
            for (String s : blacklist()) {
                if (word.contains(s))
                    return true;
            }

            // Fuzzy match
            /*
            for (String bad : blacklist) {
                if (levenshtein(word, bad) <= 1) {
                    return true;
                }
            }
             */
        }
        return false;
    }

    private static String transform(String word) {
        String copy = word.toLowerCase();
        // Remove accents
        copy = Normalizer.normalize(copy, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convert leetspeak
        StringBuilder sb = new StringBuilder();
        for (char c : copy.toCharArray()) sb.append(leetMap.getOrDefault(c, c));
        copy = sb.toString();

        // Collapse repeated characters (3+ → 1)
        return copy.replaceAll("(.)\\1{2,}", "$1");
    }

    private static int levenshtein(String x, String y) {
        if (x.isEmpty()) {
            return y.length();
        }

        if (y.isEmpty()) {
            return x.length();
        }

        int substitution = levenshtein(x.substring(1), y.substring(1)) + (x.charAt(0) == y.charAt(0) ? 0 : 1);
        int insertion = levenshtein(x, y.substring(1)) + 1;
        int deletion = levenshtein(x.substring(1), y) + 1;

        return levenshteinCostMin(substitution, insertion, deletion);
    }

    private static int levenshteinCostMin(int... numbers) {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

    public static JsonObject json() {
        JsonObject obj = new JsonObject();
        obj.add("blacklist", JsonHelper.toJsonArrayString(blacklist()));
        obj.add("whitelist", JsonHelper.toJsonArrayString(whitelist()));
        return obj;
    }
}
package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class KickAction {
    public static void kick(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        StampEntry banEntry = new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence);
        entry.kicks.addFirst(banEntry);
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, Actions.KICK, entry, banEntry, getText(banEntry));
    }

    public static Component getText(StampEntry entry) {
        if (entry != null) {
            return Component.translatable("multiplayer.disconnect.kicked").withStyle(ChatFormatting.BOLD, ChatFormatting.RED).append("\n\n")
                    .append(Component.translatableWithFallback("punishment.reason", "Reason: %s", Component.literal(entry.why).withStyle(ChatFormatting.YELLOW)));
        }
        return null;
    }

    public static Component getTextSimple(StampEntry entry) {
        if (entry != null) {
            String when = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(entry.when);
            return Component.translatableWithFallback("prohibitor.punishment.kick", "Kick").withStyle(ChatFormatting.RED).append(" - ").append(when).append("\n")
                    .append(Component.translatableWithFallback("punishment.reason", "Reason: %s", Component.literal(entry.why).withStyle(ChatFormatting.YELLOW)));
        }
        return null;
    }
}
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

public class WarnAction {
    public static void warn(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);

        StampEntry actionEntry = new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence);
        entry.warns.addFirst(actionEntry);
        ProhibitorModule.notifyServer(source, Actions.WARN, entry, actionEntry, getText(actionEntry));
        saveEntry(entry);
    }

    public static Component getText(StampEntry actionEntry) {
        if (actionEntry != null) {
            return Component.translatable("mco.warning").withStyle(ChatFormatting.BOLD, ChatFormatting.RED).append("\n\n")
                    .append(Component.translatableWithFallback("punishment.reason", "Reason: %s", Component.literal(actionEntry.why).withStyle(ChatFormatting.YELLOW)));
        }
        return null;
    }

    public static Component getTextSimple(StampEntry entry) {
        if (entry != null) {
            String when = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(entry.when);
            return Component.translatableWithFallback("prohibitor.punishment.warn", "Warn").withStyle(ChatFormatting.RED).append(" - ").append(when).append("\n")
                    .append(Component.translatableWithFallback("punishment.reason", "Reason: %s", Component.literal(entry.why).withStyle(ChatFormatting.YELLOW)));
        }
        return null;
    }
}
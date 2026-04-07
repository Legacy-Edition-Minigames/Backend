package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class WarnAction {
    public static void warn(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);

        StampEntry actionEntry = new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence);
        entry.warns.addFirst(actionEntry);
        ProhibitorModule.notifyServer(source, Actions.WARN, entry, actionEntry, getBanText(actionEntry));
        saveEntry(entry);
    }

    public static Component getBanText(StampEntry actionEntry) {
        if (actionEntry != null) {
            return Component.translatable("mco.warning").withStyle(ChatFormatting.BOLD, ChatFormatting.RED).append("\n\n")
                    .append(Component.translatableWithFallback("punishment.reason", "Reason: %s", Component.literal(actionEntry.why).withStyle(ChatFormatting.YELLOW)));
        }
        return null;
    }
}
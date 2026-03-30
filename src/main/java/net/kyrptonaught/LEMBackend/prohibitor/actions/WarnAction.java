package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

    public static Text getBanText(StampEntry actionEntry) {
        if (actionEntry != null) {
            return Text.translatable("mco.warning").formatted(Formatting.BOLD, Formatting.RED).append("\n\n")
                    .append(Text.translatableWithFallback("punishment.reason", "Reason: %s", Text.literal(actionEntry.why).formatted(Formatting.YELLOW)));
        }
        return null;
    }
}

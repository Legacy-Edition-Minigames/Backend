package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class KickAction {
    public static void kick(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        StampEntry banEntry = new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence);
        entry.kicks.addFirst(banEntry);
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, Actions.KICK, entry, banEntry, getKickText(banEntry));
    }

    public static Text getKickText(StampEntry banEntry) {
        if (banEntry != null) {
            return Text.translatable("multiplayer.disconnect.kicked").formatted(Formatting.BOLD, Formatting.RED).append("\n\n")
                    .append(Text.translatableWithFallback("punishment.reason", "Reason: %s", Text.literal(banEntry.why).formatted(Formatting.YELLOW)));
        }
        return null;
    }
}

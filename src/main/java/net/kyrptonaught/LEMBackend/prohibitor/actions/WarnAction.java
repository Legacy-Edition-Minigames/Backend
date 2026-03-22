package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.text.Text;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class WarnAction {
    public static void warn(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);

        entry.warns.addFirst(new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence));
        ProhibitorModule.notifyServer(source, uuid, "warn", Text.translatable("commands.warn.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        saveEntry(entry);
    }
}

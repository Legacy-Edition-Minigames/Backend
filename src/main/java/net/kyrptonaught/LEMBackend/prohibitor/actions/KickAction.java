package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.text.Text;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class KickAction {
    public static void kick(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        entry.kicks.addFirst(new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence));
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, uuid, "kick", Text.translatable("commands.kick.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
    }
}

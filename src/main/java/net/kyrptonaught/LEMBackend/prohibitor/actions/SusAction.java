package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.text.Text;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class SusAction {
    public static boolean sus(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);

        if (entry.sussyStatus == null) {
            entry.sussyStatus = new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence);
            saveEntry(entry);
            ProhibitorModule.notifyServer(source, uuid, "sus", Text.translatable("commands.sus.add.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
            return true;
        }
        ProhibitorModule.notifyServer(source, uuid, "sus", Text.translatable("commands.sus.add.failure", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        return false;
    }

    public static void revokeSus(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        entry.sussyStatus = null;
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, entry.associatedUUID, "unsus", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.sus.remove.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }
}

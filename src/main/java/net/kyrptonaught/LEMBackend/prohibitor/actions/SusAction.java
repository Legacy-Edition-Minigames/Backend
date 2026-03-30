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
            ProhibitorModule.notifyServer(source, Actions.SUS, entry, entry.sussyStatus, Text.empty());
            return true;
        }
        return false;
    }

    public static void revokeSus(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        entry.sussyStatus = null;
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, Actions.UNSUS, entry, new StampEntry(who, source, Instant.now(), reason), Text.empty());
    }
}

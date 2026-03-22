package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.BanEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.minecraft.text.Text;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class MuteAction {

    public static void permaMute(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        entry.mutes.addFirst(BanEntry.PermaBan(reason, who, source).addEvidence(evidence));
        ProhibitorModule.notifyServer(source, uuid, "mute", Text.translatable("commands.mute.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        saveEntry(entry);
    }

    public static void tempMute(String uuid, String who, String source, String reason, int duration_time, byte duration_type, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        entry.mutes.addFirst(BanEntry.TempBan(reason, duration_time, duration_type, who, source).addEvidence(evidence));
        ProhibitorModule.notifyServer(source, uuid, "mute", Text.translatable("commands.mute.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        saveEntry(entry);
    }

    public static void revokeMutes(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        Instant now = Instant.now();
        for (BanEntry ban : entry.mutes) {
            entry.checkBan(ban, now);
            if (!ban.expired && ban.revokedSource == null)
                ban.revoke(who, source, reason);
        }
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, entry.associatedUUID, "unmute", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.unmute.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }
}

package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.BanEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.ID_TYPE;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.minecraft.text.Text;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.*;

public class BanAction {

    public static void multiPermBan(String id_types, String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry uuidEntry = ProhibitorModule.loadUUID(uuid);
        Instant now = Instant.now();
        if (id_types.contains("_uuid_")) permaBan(uuidEntry, who, source, reason, now, evidence);
        if (id_types.contains("_name_")) permaBan(load(ID_TYPE.NAME, uuidEntry.associatedName), who, source, reason, now, evidence);
        if (id_types.contains("_ip_") && uuidEntry.associatedIP != null) permaBan(load(ID_TYPE.IP, uuidEntry.associatedIP), who, source, reason, now, evidence);
    }

    public static void multiTempBan(String id_types, String uuid, String who, String source, String reason, int duration_time, byte duration_type, String... evidence) {
        PlayerEntry uuidEntry = ProhibitorModule.loadUUID(uuid);
        Instant now = Instant.now();
        if (id_types.contains("_uuid_")) tempBan(uuidEntry, who, source, reason, duration_time, duration_type, now, evidence);
        if (id_types.contains("_name_")) tempBan(load(ID_TYPE.NAME, uuidEntry.associatedName), who, source, reason, duration_time, duration_type, now, evidence);
        if (id_types.contains("_ip_") && uuidEntry.associatedIP != null) tempBan(load(ID_TYPE.IP, uuidEntry.associatedIP), who, source, reason, duration_time, duration_type, now, evidence);
    }

    public static void permaBan(ID_TYPE type, String id, String who, String source, String reason, String... evidence) {
        permaBan(load(type, id), who, source, reason, Instant.now(), evidence);
    }

    public static void tempBan(ID_TYPE type, String id, String who, String source, String reason, int duration_time, byte duration_type, String... evidence) {
        tempBan(load(type, id), who, source, reason, duration_time, duration_type, Instant.now(), evidence);
    }

    public static void permaBan(PlayerEntry entry, String who, String source, String reason, Instant now, String... evidence) {
        entry.bans.addFirst(BanEntry.PermaBan(reason, who, source).updateWhen(now).addEvidence(evidence));
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, entry.associatedUUID, "ban", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.ban.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }

    public static void tempBan(PlayerEntry entry, String who, String source, String reason, int duration_time, byte duration_type, Instant now, String... evidence) {
        entry.bans.addFirst(BanEntry.TempBan(reason, duration_time, duration_type, who, source).updateWhen(now).addEvidence(evidence));
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, entry.associatedUUID, "ban", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.ban.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }

    public static void revokeBans(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        Instant now = Instant.now();
        for (BanEntry ban : entry.bans) {
            entry.checkBan(ban, now);
            if (!ban.expired && ban.revokedSource == null)
                ban.revoke(who, source, reason);
        }
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, entry.associatedUUID, "unban", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.unban.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }
}

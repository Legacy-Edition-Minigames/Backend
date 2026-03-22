package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.text.Text;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class WhitelistAction {
    public static boolean whitelist(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);

        if (entry.whitelistStatus == null) {
            entry.whitelistStatus = new StampEntry(who, source, Instant.now(), reason);
            saveEntry(entry);
            ProhibitorModule.notifyServer(source, uuid, "whitelist", Text.translatable("commands.whitelist.add.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
            return true;
        }
        ProhibitorModule.notifyServer(source, uuid, "whitelist", Text.translatable("commands.whitelist.add.failure", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        return false;
    }

    public static void revokeWhitelist(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        entry.whitelistStatus = null;
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, entry.associatedUUID, "unwhitelist", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.whitelist.remove.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }
}

package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.network.chat.Component;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class WhitelistAction {
    public static boolean whitelist(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);

        if (entry.whitelistStatus == null) {
            entry.whitelistStatus = new StampEntry(who, source, Instant.now(), reason);
            saveEntry(entry);
            ProhibitorModule.notifyServer(source, Actions.WHITELIST, entry, entry.whitelistStatus, Component.empty());
            return true;
        }
        return false;
    }

    public static void revokeWhitelist(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        entry.whitelistStatus = null;
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, Actions.UNWHITELIST, entry, new StampEntry(who, source, Instant.now(), reason), Component.empty());
    }
}
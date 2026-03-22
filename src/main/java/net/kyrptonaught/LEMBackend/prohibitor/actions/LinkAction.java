package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.entries.DiscordLinkEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class LinkAction {
    public static boolean link(String uuid, long discordID, String source) {
        PlayerEntry entry = loadUUID(uuid);

        if (entry.discordLink == null) {
            entry.discordLink = new DiscordLinkEntry(Instant.now(), discordID, source);
            saveEntry(entry);
            return true;
        }
        return false;
    }
}

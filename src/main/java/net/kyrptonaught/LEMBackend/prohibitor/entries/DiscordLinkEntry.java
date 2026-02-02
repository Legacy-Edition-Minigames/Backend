package net.kyrptonaught.LEMBackend.prohibitor.entries;

import java.time.Instant;

public record DiscordLinkEntry(Instant dateLinked, long discordID, String server) {
}

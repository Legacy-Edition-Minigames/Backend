package net.kyrptonaught.LEMBackend.prohibitor.entries;

import java.time.Instant;

public record StampEntry(String who, String where, Instant when, String why) {
}

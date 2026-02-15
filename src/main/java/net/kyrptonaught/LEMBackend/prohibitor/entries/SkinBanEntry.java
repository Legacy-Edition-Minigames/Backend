package net.kyrptonaught.LEMBackend.prohibitor.entries;

import java.time.Instant;

public class SkinBanEntry {
    public StampEntry banSource;
    public String skin;

    public SkinBanEntry(String skin, String who, String source, String reason) {
        banSource = new StampEntry(who, source, Instant.now(), reason);
        this.skin = skin;
    }

    public SkinBanEntry addEvidence(String... evidence) {
        banSource.attachEvidence(evidence);
        return this;
    }

}

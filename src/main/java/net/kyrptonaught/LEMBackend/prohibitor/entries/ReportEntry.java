package net.kyrptonaught.LEMBackend.prohibitor.entries;

import java.time.Instant;

public class ReportEntry implements Entry {
    public StampEntry source;

    public String link;

    public ReportEntry(String link, String who, String source, String reason) {
        this.source = new StampEntry(who, source, Instant.now(), reason);
        this.link = link;
    }

    public ReportEntry addEvidence(String... evidence) {
        source.attachEvidence(evidence);
        return this;
    }
}

package net.kyrptonaught.LEMBackend.prohibitor.entries;

import net.kyrptonaught.LEMBackend.FileHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class StampEntry {
    public final String who;
    public final String where;
    public Instant when;
    public final String why;
    public final List<String> evidence = new ArrayList<>();
    public String punishment_id;

    public StampEntry(String who, String where, Instant when, String why) {
        this.who = who;
        this.where = where;
        this.when = when;
        this.why = why;
        this.punishment_id = FileHelper.hashFile((who + where + why + when.toString()).getBytes());
    }

    public StampEntry attachEvidence(String... evidence) {
        this.evidence.addAll(List.of(evidence));
        return this;
    }

    public StampEntry updateWhen(Instant now) {
        this.when = now;
        genHash();
        return this;
    }

    public void genHash() {
        this.punishment_id = FileHelper.hashFile((who + where + why + when.toString()).getBytes());
    }
}

package net.kyrptonaught.LEMBackend.prohibitor.entries;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BanEntry {
    public StampEntry banSource;

    public boolean permanent;

    public int duration_time;
    public byte duration_type;
    public String duration_pretty;
    public boolean expired;

    public StampEntry revokedSource;

    private BanEntry(StampEntry stamp) {
        this.banSource = stamp;
    }

    public static BanEntry PermaBan(String reason, String who, String source) {
        return PermaBan(new StampEntry(who, source, Instant.now(), reason));
    }

    public static BanEntry PermaBan(StampEntry stamp) {
        BanEntry entry = new BanEntry(stamp);
        entry.permanent = true;
        return entry;
    }

    public static BanEntry TempBan(String reason, int duration_time, byte duration_type, String who, String source) {
        return TempBan(new StampEntry(who, source, Instant.now(), reason), duration_time, duration_type);
    }

    public static BanEntry TempBan(StampEntry stamp, int duration_time, byte duration_type) {
        BanEntry entry = new BanEntry(stamp);
        entry.permanent = false;
        entry.duration_time = duration_time;
        entry.duration_type = duration_type;
        entry.duration_pretty = duration_time + " " + ChronoUnit.values()[duration_type].toString();
        return entry;
    }

    public BanEntry addEvidence(String... evidence) {
        banSource.attachEvidence(evidence);
        return this;
    }

    public BanEntry updateWhen(Instant now) {
        banSource.updateWhen(now);
        return this;
    }

    public void revoke(String who, String source, String reason) {
        this.revokedSource = new StampEntry(who, source, Instant.now(), reason);
    }

    public void expire() {
        this.expired = true;
    }

    public MutableText getDurationText() {
        if (permanent) return Text.translatable("punishment.permenant");
        return Text.translatable("punishment.duration." + ChronoUnit.values()[duration_type].toString().toLowerCase(), duration_time);
    }

    public Duration getDuration() {
        return Duration.of(duration_time, ChronoUnit.values()[duration_type]);
    }
}

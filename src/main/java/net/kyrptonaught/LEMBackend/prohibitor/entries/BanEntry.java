package net.kyrptonaught.LEMBackend.prohibitor.entries;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BanEntry implements Entry {
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

    public MutableText getDurationText(Instant now) {
        if (permanent) return Text.translatable("team.collision.never");

        long minutes = getRemaining(now);
        long hours = (minutes / (60)) % 24;
        long days = minutes / (60 * 24) % 365;
        long years = minutes / (60 * 24 * 365);

        if (minutes == 0) minutes = 1;

        MutableText text = Text.empty();
        if (years > 0) text.append(Text.translatableWithFallback("gui.years", "%s year(s)", years));
        if (days > 0) text.append(Text.translatable("gui.days", years));
        if (hours > 0) text.append(Text.translatable("gui.hours", years));
        text.append(Text.translatable("gui.minutes", minutes));

        return text;
    }

    public Duration getDuration() {
        return Duration.of(duration_time, ChronoUnit.values()[duration_type]);
    }

    public long getRemaining(Instant now) {
        if (permanent) return -1;
        return now.until(banSource.when.plus(getDuration()), ChronoUnit.MINUTES);
    }
}

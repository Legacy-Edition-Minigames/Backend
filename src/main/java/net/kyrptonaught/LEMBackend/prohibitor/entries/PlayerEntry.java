package net.kyrptonaught.LEMBackend.prohibitor.entries;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PlayerEntry {
    public String uuid;
    public String mcname;

    public DiscordLinkEntry discordLink;

    public StampEntry whitelistStatus;
    public StampEntry sussyStatus;

    public Instant firstSeen;
    public Instant lastSeen;

    public List<BanEntry> bans = new ArrayList<>();
    public List<BanEntry> mutes = new ArrayList<>();
    public List<StampEntry> warns = new ArrayList<>();
    public List<StampEntry> kicks = new ArrayList<>();


    public BanEntry isActiveBan(Instant now) {
        for (BanEntry ban : bans) {
            if (ban.expired || ban.revokedSource != null) continue;
            if (ban.permanent) return ban;

            if (now.isAfter(ban.banSource.when().plus(ban.getDuration()))) ban.expire();
            else return ban;
        }

        return null;
    }

    public BanEntry isActiveMute(Instant now) {
        for (BanEntry ban : mutes) {
            if (ban.expired || ban.revokedSource != null) continue;
            if (ban.permanent) return ban;

            if (now.isAfter(ban.banSource.when().plus(ban.getDuration()))) ban.expire();
            else return ban;
        }

        return null;
    }
}

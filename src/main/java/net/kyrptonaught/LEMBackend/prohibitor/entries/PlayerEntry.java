package net.kyrptonaught.LEMBackend.prohibitor.entries;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerEntry {
    public ID_TYPE id_type;

    public String id;
    public String associatedName;
    public String associatedUUID;
    public String associatedIP;

    public DiscordLinkEntry discordLink;

    public StampEntry whitelistStatus;
    public StampEntry sussyStatus;

    public Instant firstSeen;
    public Instant lastSeen;

    public Set<String> associations = new HashSet<>();

    public List<BanEntry> bans = new ArrayList<>();
    public List<BanEntry> mutes = new ArrayList<>();
    public List<StampEntry> warns = new ArrayList<>();
    public List<StampEntry> kicks = new ArrayList<>();
    public List<SkinBanEntry> skinBans = new ArrayList<>();

    public BanEntry isActiveBan(Instant now) {
        for (BanEntry ban : bans) {
            if (ban.expired || ban.revokedSource != null) continue;
            if (ban.permanent) return ban;

            if (now.isAfter(ban.banSource.when.plus(ban.getDuration()))) ban.expire();
            else return ban;
        }

        return null;
    }

    public BanEntry isActiveMute(Instant now) {
        for (BanEntry ban : mutes) {
            if (ban.expired || ban.revokedSource != null) continue;
            if (ban.permanent) return ban;

            if (now.isAfter(ban.banSource.when.plus(ban.getDuration()))) ban.expire();
            else return ban;
        }

        return null;
    }

    public SkinBanEntry isActiveSkinBan(String skin) {
        for (SkinBanEntry ban : skinBans) if (skin.equals(ban.skin)) return ban;
        return null;
    }

    public void checkBan(BanEntry ban, Instant now) {
        if (ban.permanent || ban.expired) return;
        if (now.isAfter(ban.banSource.when.plus(ban.getDuration()))) ban.expire();
    }
}

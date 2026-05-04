package net.kyrptonaught.LEMBackend.prohibitor.entries;

import com.google.gson.JsonElement;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeOut;
import net.kyrptonaught.LEMBackend.prohibitor.actions.*;

import java.time.Instant;
import java.util.*;

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

    public List<ReportEntry> reports = new ArrayList<>();

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

    public void addBan(BanEntry entry) {
        for (BanEntry ban : bans) if (ban.banSource.punishment_id.equals(entry.banSource.punishment_id)) return;
        bans.add(entry);
    }

    public void addMute(BanEntry entry) {
        for (BanEntry ban : mutes) if (ban.banSource.punishment_id.equals(entry.banSource.punishment_id)) return;
        mutes.add(entry);
    }

    public void addKick(StampEntry entry) {
        for (StampEntry ban : kicks) if (ban.punishment_id.equals(entry.punishment_id)) return;
        kicks.add(entry);
    }

    public void addWarn(StampEntry entry) {
        for (StampEntry ban : warns) if (ban.punishment_id.equals(entry.punishment_id)) return;
        warns.add(entry);
    }

    public void markAllAck() {
        for (BanEntry entry : bans) entry.banSource.markAcknowledged();
        for (BanEntry entry : mutes) entry.banSource.markAcknowledged();
        for (StampEntry entry : warns) entry.markAcknowledged();
        for (StampEntry entry : kicks) entry.markAcknowledged();
        for (SkinBanEntry entry : skinBans) entry.banSource.markAcknowledged();
    }

    public void getUnAcknowledged(HashMap<String, JsonElement> map) {
        Instant now = Instant.now();

        for (BanEntry entry : bans) if (!entry.banSource.acknowledged) map.put(entry.banSource.punishment_id, BridgeOut.encodeText(BanAction.getTextSimple(entry, now)));
        for (BanEntry entry : mutes) if (!entry.banSource.acknowledged) map.put(entry.banSource.punishment_id, BridgeOut.encodeText(MuteAction.getTextSimple(entry, now)));
        for (StampEntry entry : warns) if (!entry.acknowledged) map.put(entry.punishment_id, BridgeOut.encodeText(WarnAction.getTextSimple(entry)));
        for (StampEntry entry : kicks) if (!entry.acknowledged) map.put(entry.punishment_id, BridgeOut.encodeText(KickAction.getTextSimple(entry)));
        for (SkinBanEntry entry : skinBans) if (!entry.banSource.acknowledged) map.put(entry.banSource.punishment_id, BridgeOut.encodeText(SkinBanAction.getTextSimple(entry)));
    }
}
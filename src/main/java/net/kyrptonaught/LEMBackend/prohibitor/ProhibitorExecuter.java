package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeIn;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeOut;
import net.kyrptonaught.LEMBackend.prohibitor.entries.*;
import net.minecraft.text.Text;
import net.minecraft.util.LenientJsonParser;

import java.time.Instant;
import java.util.Base64;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.*;

public class ProhibitorExecuter {

    public static void permaBan(ID_TYPE type, String id, String who, String source, String reason, String... evidence) {
        permaBan(load(type, id), who, source, reason, Instant.now(), evidence);
    }

    public static void tempBan(ID_TYPE type, String id, String who, String source, String reason, int duration_time, byte duration_type, String... evidence) {
        tempBan(load(type, id), who, source, reason, duration_time, duration_type, Instant.now(), evidence);
    }

    public static void permaBan(PlayerEntry entry, String who, String source, String reason, Instant now, String... evidence) {
        entry.bans.addFirst(BanEntry.PermaBan(reason, who, source).updateWhen(now).addEvidence(evidence));
        saveEntry(entry);
        notifyServer(source, entry.associatedUUID, "ban", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.ban.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }

    public static void tempBan(PlayerEntry entry, String who, String source, String reason, int duration_time, byte duration_type,Instant now, String... evidence) {
        entry.bans.addFirst(BanEntry.TempBan(reason, duration_time, duration_type, who, source).updateWhen(now).addEvidence(evidence));
        saveEntry(entry);
        notifyServer(source, entry.associatedUUID, "ban", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.ban.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }

    public static void permaMute(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        entry.mutes.addFirst(BanEntry.PermaBan(reason, who, source).addEvidence(evidence));
        notifyServer(source, uuid, "mute", Text.translatable("commands.mute.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        saveEntry(entry);
    }

    public static void tempMute(String uuid, String who, String source, String reason, int duration_time, byte duration_type, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        entry.mutes.addFirst(BanEntry.TempBan(reason, duration_time, duration_type, who, source).addEvidence(evidence));
        notifyServer(source, uuid, "mute", Text.translatable("commands.mute.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        saveEntry(entry);
    }

    public static void warn(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);

        entry.warns.addFirst(new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence));
        notifyServer(source, uuid, "warn", Text.translatable("commands.warn.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        saveEntry(entry);
    }

    public static void kick(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        entry.kicks.addFirst(new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence));
        saveEntry(entry);
        notifyServer(source, uuid, "kick", Text.translatable("commands.kick.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
    }

    public static boolean whitelist(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);

        if (entry.whitelistStatus == null) {
            entry.whitelistStatus = new StampEntry(who, source, Instant.now(), reason);
            saveEntry(entry);
            notifyServer(source, uuid, "whitelist", Text.translatable("commands.whitelist.add.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
            return true;
        }
        notifyServer(source, uuid, "whitelist", Text.translatable("commands.whitelist.add.failure", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        return false;
    }

    public static boolean sus(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);

        if (entry.sussyStatus == null) {
            entry.sussyStatus = new StampEntry(who, source, Instant.now(), reason).attachEvidence(evidence);
            saveEntry(entry);
            notifyServer(source, uuid, "sus", Text.translatable("commands.sus.add.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
            return true;
        }
        notifyServer(source, uuid, "sus", Text.translatable("commands.sus.add.failure", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
        return false;
    }

    public static void skinBan(String uuid, String skin, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        entry.skinBans.add(new SkinBanEntry(skin, who, source, reason).addEvidence(evidence));
        ProhibitorModule.generateSkinRender(skin);

        saveEntry(entry);
        notifyServer(source, uuid, "kick", Text.translatable("commands.skinban.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
    }

    public static boolean link(String uuid, long discordID, String source) {
        PlayerEntry entry = loadUUID(uuid);

        if (entry.discordLink == null) {
            entry.discordLink = new DiscordLinkEntry(Instant.now(), discordID, source);
            saveEntry(entry);
            return true;
        }
        return false;
    }

    public static void revokeBans(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        Instant now = Instant.now();
        for (BanEntry ban : entry.bans) {
            entry.checkBan(ban, now);
            if (!ban.expired && ban.revokedSource == null)
                ban.revoke(who, source, reason);
        }

        saveEntry(entry);
        notifyServer(source, entry.associatedUUID, "unban", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.pardon.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }

    public static void revokeMutes(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        Instant now = Instant.now();
        for (BanEntry ban : entry.mutes) {
            entry.checkBan(ban, now);
            if (!ban.expired && ban.revokedSource == null)
                ban.revoke(who, source, reason);
        }
        saveEntry(entry);
        notifyServer(source, entry.associatedUUID, "unmute", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.unmute.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }

    public static void revokeWhitelist(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        entry.whitelistStatus = null;
        saveEntry(entry);
        notifyServer(source, entry.associatedUUID, "unwhitelist", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.whitelist.remove.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }

    public static void revokeSus(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        entry.sussyStatus = null;
        saveEntry(entry);
        notifyServer(source, entry.associatedUUID, "unsus", Text.literal(entry.id_type.name() + " ").append(Text.translatable("commands.sus.remove.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who)));
    }

    public static void revokeSkinBan(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        entry.skinBans.clear();
        saveEntry(entry);
        notifyServer(source, uuid, "unskinban", Text.translatable("commands.skinban.success", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
    }

    public static void notifyServer(String source, String uuid, String action, Text reason) {
        notifyServer(source,uuid,action,reason, null);
    }

    public static void notifyServer(String source, String uuid, String action, Text reason, JsonObject custom) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", uuid);
        obj.addProperty("action", action);
        BridgeOut.encodeText(obj, "reason", reason);
        if(custom != null) obj.add("custom", custom);

        BridgeOut.sendMessageToAllServers("prohibitor", obj);
        BridgeIn.sendLogMessage(source, reason);
    }
}

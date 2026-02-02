package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.IO;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeIn;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeOut;
import net.kyrptonaught.LEMBackend.discordBridge.PatreonTier;
import net.kyrptonaught.LEMBackend.prohibitor.entries.BanEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.DiscordLinkEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.util.Map;

public class ProhibitorModule extends Module {
    public ProhibitorModule() {
        super("prohibitor");
    }

    public Text canPlayerJoin(String uuid, String whitelistStatus) {
        PlayerEntry entry = loadPlayer(uuid);
        Instant now = Instant.now();
        BanEntry banEntry = entry.isActiveBan(now);

        if (banEntry != null) {
            return Text.translatable("multiplayer.disconnect.banned").formatted(Formatting.BOLD, Formatting.RED).append("\n\n")
                    .append(Text.translatable("punishment.reason", Text.literal(banEntry.banSource.why()).formatted(Formatting.YELLOW))).append("\n")
                    .append(Text.translatable("punishment.expires", banEntry.getDurationText().formatted(Formatting.YELLOW)));
        }

        if (whitelistStatus.equals(WhitelistStatus.WHITELIST.name()))
            if (entry.whitelistStatus == null) return Text.translatable("multiplayer.status.cannot_connect").append("\n").append(Text.translatable("multiplayer.disconnect.not_whitelisted"));

        if (whitelistStatus.equals(WhitelistStatus.DISCORD.name()))
            if (entry.discordLink == null) return Text.translatable("multiplayer.status.cannot_connect").append("\n").append(Text.translatable("prohibitor.discordlink.required"));

        if (whitelistStatus.equals(WhitelistStatus.PATREONS.name()))
            if (LEMBackend.BridgeModule.module.getPatreonTier(entry.discordLink.discordID()) == PatreonTier.NONE) return Text.translatable("multiplayer.status.cannot_connect").append("\n").append(Text.translatable("prohibitor.patreon.required"));

        if (entry.firstSeen == null) entry.firstSeen = now;
        entry.lastSeen = now;
        entry.isActiveMute(now);
        savePlayer(entry);
        return null;
    }

    public boolean canPlayerChat(String uuid) {
        PlayerEntry entry = loadPlayer(uuid);

        return entry.isActiveMute(Instant.now()) == null;
    }

    public void permaBan(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadPlayer(uuid);
        entry.bans.addFirst(BanEntry.PermaBan(reason, who, source));
        savePlayer(entry);
        notifyServer(source, uuid, "ban", Text.translatable("commands.ban.success", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
    }

    public void tempBan(String uuid, String who, String source, String reason, int duration_time, byte duration_type) {
        PlayerEntry entry = loadPlayer(uuid);
        entry.bans.addFirst(BanEntry.TempBan(reason, duration_time, duration_type, who, source));
        savePlayer(entry);
        notifyServer(source, uuid, "ban", Text.translatable("commands.ban.success", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
    }

    public void permaMute(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadPlayer(uuid);
        entry.mutes.addFirst(BanEntry.PermaBan(reason, who, source));
        notifyServer(source, uuid, "mute", Text.translatable("commands.mute.success", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
        savePlayer(entry);
    }

    public void tempMute(String uuid, String who, String source, String reason, int duration_time, byte duration_type) {
        PlayerEntry entry = loadPlayer(uuid);
        entry.mutes.addFirst(BanEntry.TempBan(reason, duration_time, duration_type, who, source));
        notifyServer(source, uuid, "mute", Text.translatable("commands.mute.success", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
        savePlayer(entry);
    }

    public void warn(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadPlayer(uuid);

        entry.warns.addFirst(new StampEntry(who, source, Instant.now(), reason));
        notifyServer(source, uuid, "warn", Text.translatable("commands.warn.success", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
        savePlayer(entry);
    }

    public void kick(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadPlayer(uuid);
        entry.kicks.addFirst(new StampEntry(who, source, Instant.now(), reason));
        savePlayer(entry);
        notifyServer(source, uuid, "kick", Text.translatable("commands.kick.success", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
    }

    public boolean whitelist(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadPlayer(uuid);

        if (entry.whitelistStatus == null) {
            entry.whitelistStatus = new StampEntry(who, source, Instant.now(), reason);
            savePlayer(entry);
            notifyServer(source, uuid, "whitelist", Text.translatable("commands.whitelist.add.success", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
            return true;
        }
        notifyServer(source, uuid, "whitelist", Text.translatable("commands.whitelist.add.failure", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
        return false;
    }

    public boolean sus(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadPlayer(uuid);

        if (entry.sussyStatus == null) {
            entry.sussyStatus = new StampEntry(who, source, Instant.now(), reason);
            savePlayer(entry);
            notifyServer(source, uuid, "sus", Text.translatable("commands.sus.add.success", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
            return true;
        }
        notifyServer(source, uuid, "sus", Text.translatable("commands.sus.add.failure", Text.literal(entry.mcname), reason).append("\nBy: ").append(who));
        return false;
    }

    public void unSus(String uuid) {
        PlayerEntry entry = loadPlayer(uuid);
        entry.sussyStatus = null;
        savePlayer(entry);
    }

    public boolean link(String uuid, long discordID, String source) {
        PlayerEntry entry = loadPlayer(uuid);

        if (entry.discordLink == null) {
            entry.discordLink = new DiscordLinkEntry(Instant.now(), discordID, source);
            savePlayer(entry);
            return true;
        }
        return false;
    }

    public PlayerEntry loadPlayer(String uuid) {
        uuid = uuid.replaceAll("-", "");
        PlayerEntry entry = readFileJson(LEMBackend.gson, "players/" + uuid + ".json", PlayerEntry.class);
        if (entry == null) {
            entry = new PlayerEntry();
            entry.mcname = getNameFromUUID(uuid);
        }
        entry.uuid = uuid;
        return entry;
    }

    public void savePlayer(PlayerEntry entry) {
        writeFileJson(LEMBackend.gson, "players/" + entry.uuid + ".json", entry);
    }

    public void notifyServer(String source, String uuid, String action, Text reason) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", uuid);
        obj.addProperty("action", action);
        BridgeOut.encodeText(obj, "reason", reason);

        BridgeOut.sendMessageToAllServers("prohibitor", obj);
        BridgeIn.sendLogMessage(source, reason);
    }


    @Override
    public void load(Gson gson) {
        FileHelper.createDir(savePath.resolve("players"));
        LinkingManager.load(readFileJson(gson, "discordLinks.json", Map.class));
    }

    @Override
    public void save(Gson gson) {
        FileHelper.createDir(savePath.resolve("players"));
        writeFileJson(gson, "discordLinks.json", LinkingManager.getSave());
    }

    public static String getUUIDFromName(String name) {
        return IO.getValue("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name, "id");
    }

    public static String getNameFromUUID(String uuid) {
        return IO.getValue("https://api.minecraftservices.com/minecraft/profile/lookup/" + uuid, "name");
    }
}
package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.util.UndashedUuid;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.IO;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeOut;
import net.kyrptonaught.LEMBackend.discordBridge.PatreonTier;
import net.kyrptonaught.LEMBackend.prohibitor.entries.BanEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.ID_TYPE;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.SkinBanEntry;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.LenientJsonParser;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public class ProhibitorModule extends Module {
    public ProhibitorModule() {
        super("prohibitor");
    }

    public JsonObject getJoinStatus(String uuid, String name, String ip, String skin, String whitelistStatus) {
        JsonObject response = new JsonObject();

        Instant now = Instant.now();

        PlayerEntry uuidEntry = load(ID_TYPE.UUID, uuid);
        PlayerEntry ipEntry = load(ID_TYPE.IP, ip);
        PlayerEntry nameEntry = load(ID_TYPE.NAME, name);

        Text banStatus = canPlayerJoin(uuidEntry, ipEntry, nameEntry, now, whitelistStatus);
        response.addProperty("isBanned", banStatus != null);
        if (banStatus != null) BridgeOut.encodeText(response, "banMessage", banStatus);

        Text muteStatus = checkPlayerEntryMutes(uuidEntry, now);
        response.addProperty("isMuted", muteStatus != null);
        if (muteStatus != null) BridgeOut.encodeText(response, "muteMessage", muteStatus);

        Text skinStatus = checkPlayerSkinBans(uuidEntry, skin);
        response.addProperty("isSkinBanned", skinStatus != null);
        if (skinStatus != null) BridgeOut.encodeText(response, "skinMessage", skinStatus);

        saveEntry(uuidEntry);
        saveEntry(ipEntry);
        saveEntry(nameEntry);

        response.addProperty("success", true);
        return response;
    }

    private Text canPlayerJoin(PlayerEntry uuidEntry, PlayerEntry ipEntry, PlayerEntry nameEntry, Instant now, String whitelistStatus) {
        Text result = checkPlayerEntryBans(uuidEntry, whitelistStatus, now);
        if (result != null) return result;

        Text result2 = checkPlayerEntryBans(ipEntry, whitelistStatus, now);
        if (result2 != null) return result2;

        Text result3 = checkPlayerEntryBans(nameEntry, whitelistStatus, now);
        if (result3 != null) return result3;


        if (uuidEntry.firstSeen == null) uuidEntry.firstSeen = now;
        uuidEntry.associatedName = nameEntry.id;
        uuidEntry.associatedUUID = uuidEntry.id;
        uuidEntry.associatedIP = ipEntry.id;
        uuidEntry.lastSeen = now;
        uuidEntry.associations.add("ip:" + ipEntry.id);
        uuidEntry.associations.add("name:" + nameEntry.id);

        if (ipEntry.firstSeen == null) ipEntry.firstSeen = now;
        ipEntry.associatedName = nameEntry.id;
        ipEntry.associatedUUID = uuidEntry.id;
        ipEntry.associatedIP = ipEntry.id;
        ipEntry.lastSeen = now;
        ipEntry.associations.add("uuid:" + uuidEntry.id);
        ipEntry.associations.add("name:" + nameEntry.id);

        if (nameEntry.firstSeen == null) nameEntry.firstSeen = now;
        nameEntry.associatedName = nameEntry.id;
        nameEntry.associatedUUID = uuidEntry.id;
        nameEntry.associatedIP = ipEntry.id;
        nameEntry.lastSeen = now;
        nameEntry.associations.add("uuid:" + uuidEntry.id);
        nameEntry.associations.add("ip:" + ipEntry.id);

        saveEntry(uuidEntry);
        saveEntry(ipEntry);
        saveEntry(nameEntry);
        return null;
    }

    public boolean canPlayerChat(String uuid) {
        PlayerEntry entry = loadUUID(uuid);

        return entry.isActiveMute(Instant.now()) == null;
    }

    private Text checkPlayerSkinBans(PlayerEntry entry, String skin) {
        SkinBanEntry banEntry = entry.isActiveSkinBan(skin);
        if (banEntry != null) {
            return Text.translatable("gui.banned.skin.title").formatted(Formatting.BOLD, Formatting.RED).append("\n\n")
                    .append(Text.translatable("punishment.reason", Text.literal(banEntry.banSource.why).formatted(Formatting.YELLOW)));
        }
        return null;
    }

    private Text checkPlayerEntryBans(PlayerEntry entry, String whitelistStatus, Instant now) {
        BanEntry banEntry = entry.isActiveBan(now);
        if (banEntry != null) {
            return Text.translatable("multiplayer.disconnect.banned").formatted(Formatting.BOLD, Formatting.RED).append("\n\n")
                    .append(Text.translatable("punishment.reason", Text.literal(banEntry.banSource.why).formatted(Formatting.YELLOW))).append("\n")
                    .append(Text.translatable("punishment.expires", banEntry.getDurationText().formatted(Formatting.YELLOW)));
        }

        if (entry.id_type == ID_TYPE.UUID) {
            if (whitelistStatus.equals(WhitelistStatus.WHITELIST.name()))
                if (entry.whitelistStatus == null) return Text.translatable("multiplayer.status.cannot_connect").append("\n").append(Text.translatable("multiplayer.disconnect.not_whitelisted"));

            if (whitelistStatus.equals(WhitelistStatus.DISCORD.name()))
                if (entry.discordLink == null) return Text.translatable("multiplayer.status.cannot_connect").append("\n").append(Text.translatable("prohibitor.discordlink.required"));

            if (whitelistStatus.equals(WhitelistStatus.PATREONS.name()))
                if (LEMBackend.BridgeModule.module.getPatreonTier(entry.discordLink.discordID()) == PatreonTier.NONE) return Text.translatable("multiplayer.status.cannot_connect").append("\n").append(Text.translatable("prohibitor.patreon.required"));
        }
        return null;
    }

    private Text checkPlayerEntryMutes(PlayerEntry entry, Instant now) {
        BanEntry banEntry = entry.isActiveMute(now);
        if (banEntry != null) {
            return Text.translatable("prohibitor.mute.cannotsent").formatted(Formatting.BOLD, Formatting.RED).append("\n\n")
                    .append(Text.translatable("punishment.reason", Text.literal(banEntry.banSource.why).formatted(Formatting.YELLOW))).append("\n")
                    .append(Text.translatable("punishment.expires", banEntry.getDurationText().formatted(Formatting.YELLOW)));
        }
        return null;
    }

    public static void multiPermBan(String id_types, String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry uuidEntry = loadUUID(uuid);
        Instant now = Instant.now();
        if (id_types.contains("_uuid_")) ProhibitorExecuter.permaBan(uuidEntry, who, source, reason, now, evidence);
        if (id_types.contains("_name_")) ProhibitorExecuter.permaBan(load(ID_TYPE.NAME, uuidEntry.associatedName), who, source, reason, now, evidence);
        if (id_types.contains("_ip_") && uuidEntry.associatedIP != null) ProhibitorExecuter.permaBan(load(ID_TYPE.IP, uuidEntry.associatedIP), who, source, reason, now, evidence);
    }

    public static void multiTempBan(String id_types, String uuid, String who, String source, String reason, int duration_time, byte duration_type, String... evidence) {
        PlayerEntry uuidEntry = loadUUID(uuid);
        Instant now = Instant.now();
        if (id_types.contains("_uuid_")) ProhibitorExecuter.tempBan(uuidEntry, who, source, reason, duration_time, duration_type, now, evidence);
        if (id_types.contains("_name_")) ProhibitorExecuter.tempBan(load(ID_TYPE.NAME, uuidEntry.associatedName), who, source, reason, duration_time, duration_type, now, evidence);
        if (id_types.contains("_ip_") && uuidEntry.associatedIP != null) ProhibitorExecuter.tempBan(load(ID_TYPE.IP, uuidEntry.associatedIP), who, source, reason, duration_time, duration_type, now, evidence);
    }

    public static void multiRevoke(String id_types, String uuid, String who, String source, String reason) {
        if (id_types.contains("_b_")) ProhibitorExecuter.revokeBans(uuid, who, source, reason);
        if (id_types.contains("_m_")) ProhibitorExecuter.revokeMutes(uuid, who, source, reason);
        if (id_types.contains("_sb_")) ProhibitorExecuter.revokeSkinBan(uuid, who, source, reason);
        if (id_types.contains("_wl_")) ProhibitorExecuter.revokeWhitelist(uuid, who, source, reason);
        if (id_types.contains("_ss_")) ProhibitorExecuter.revokeSus(uuid, who, source, reason);
    }

    public static boolean skinBan(String uuid, String who, String source, String reason, String... evidence) {
        String url = getPlayerSkin(uuid);
        if (url == null) return false;
        generateSkinRender(url);
        ProhibitorExecuter.skinBan(uuid, url, who, source, reason, evidence);
        return true;
    }

    public static PlayerEntry loadUUID(String uuid) {
        PlayerEntry entry = load(ID_TYPE.UUID, uuid);
        if (entry.associatedName == null) entry.associatedName = getNameFromUUID(uuid);
        return entry;
    }

    public static PlayerEntry load(ID_TYPE type, String id) {
        if (type == ID_TYPE.UUID) id = id.replaceAll("-", "");

        PlayerEntry entry = LEMBackend.ProhibitorModule.module.readFileJson(LEMBackend.gson, type.name() + "/" + id + ".json", PlayerEntry.class);
        if (entry == null) {
            entry = new PlayerEntry();
            entry.id_type = type;
        }

        entry.id = id;
        return entry;
    }

    public static void saveEntry(PlayerEntry entry) {
        LEMBackend.ProhibitorModule.module.writeFileJson(LEMBackend.gson, entry.id_type + "/" + entry.id + ".json", entry);
    }

    @Override
    public void load(Gson gson) {
        for (ID_TYPE idType : ID_TYPE.values()) FileHelper.createDir(savePath.resolve(idType.name()));
        FileHelper.createDir(savePath.resolve("EVIDENCE"));
        FileHelper.createDir(savePath.resolve("SKINRENDERS"));
        LinkingManager.load(readFileJson(gson, "discordLinks.json", Map.class));
    }

    @Override
    public void save(Gson gson) {
        for (ID_TYPE idType : ID_TYPE.values()) FileHelper.createDir(savePath.resolve(idType.name()));
        FileHelper.createDir(savePath.resolve("EVIDENCE"));
        FileHelper.createDir(savePath.resolve("SKINRENDERS"));
        writeFileJson(gson, "discordLinks.json", LinkingManager.getSave());
    }

    public static String downloadEvidence(String url, String uuid, String fileExtension) {
        String file = uuid + "---" + genRandomID() + "." + fileExtension;
        FileHelper.download(url, getEvidiencePath(file));
        return file;
    }

    public static Path getEvidiencePath(String file) {
        return LEMBackend.ProhibitorModule.module.savePath.resolve("EVIDENCE").resolve(file);
    }

    public static String getPlayerSkin(String uuid) {
        if (LEMBackend.minecraftServer.getApiServices().sessionService() instanceof YggdrasilMinecraftSessionService sessionService) {
            ProfileResult profile = sessionService.fetchProfile(UndashedUuid.fromString(uuid), true);
            if (profile == null) return null;
            Property prop = Iterables.getFirst(profile.profile().properties().get("textures"), null);
            if (prop == null) return null;
            return LenientJsonParser.parse(new String(Base64.getDecoder().decode(prop.value()))).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

        }
        return null;
    }

    public static void generateSkinRender(String url) {
        String api = "https://starlightskins.lunareclipse.studio/render/custom/steve/full?wideModel=https://raw.githubusercontent.com/kyrptonaught/Minigame-Resources/refs/heads/2.0/double.obj&cameraPosition={%22x%22:%220%22,%22y%22:%2220%22,%22z%22:%22-40%22}&skinUrl=" + url;
        FileHelper.download(api, getSkinRenderPath(url));
    }

    public static Path getSkinRenderPath(String url) {
        return LEMBackend.ProhibitorModule.module.savePath.resolve("SKINRENDERS").resolve(url.substring(38) + ".png");
    }

    public static String getUUIDFromName(String name) {
        return IO.getValue("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name, "id");
    }

    public static String getNameFromUUID(String uuid) {
        return IO.getValue("https://api.minecraftservices.com/minecraft/profile/lookup/" + uuid, "name");
    }

    public static String genRandomID() {
        return RandomStringUtils.secure().nextAlphanumeric(15);
    }
}
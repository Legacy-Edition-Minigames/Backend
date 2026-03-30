package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.IO;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.Module;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeModule;
import net.kyrptonaught.LEMBackend.discordBridge.BridgeOut;
import net.kyrptonaught.LEMBackend.discordBridge.PatreonTier;
import net.kyrptonaught.LEMBackend.prohibitor.actions.*;
import net.kyrptonaught.LEMBackend.prohibitor.discordCommands.ViewCommand;
import net.kyrptonaught.LEMBackend.prohibitor.entries.*;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ProhibitorModule extends Module {
    public ProhibitorModule() {
        super("prohibitor");
        ChatFilter.genWords();
    }

    public static JsonObject getJoinStatus(String uuid, String name, String ip, String skin, String whitelistStatus) {
        JsonObject response = new JsonObject();

        Instant now = Instant.now();

        PlayerEntry uuidEntry = load(ID_TYPE.UUID, uuid);
        PlayerEntry ipEntry = load(ID_TYPE.IP, ip);
        PlayerEntry nameEntry = load(ID_TYPE.NAME, name);

        Text banStatus = canPlayerJoin(uuidEntry, ipEntry, nameEntry, now, whitelistStatus);
        response.addProperty("isBanned", banStatus != null);
        if (banStatus != null) BridgeOut.encodeText(response, "banMessage", banStatus);

        BanEntry muteEntry = uuidEntry.isActiveMute(now);
        response.addProperty("isMuted", muteEntry != null);
        if (muteEntry != null) {
            BridgeOut.encodeText(response, "muteMessage", MuteAction.getMuteText(muteEntry, now));
            response.addProperty("muteDuration", muteEntry.getRemaining(now));
        }

        Text skinStatus = checkPlayerSkinBans(uuidEntry, skin);
        response.addProperty("isSkinBanned", skinStatus != null);
        if (skinStatus != null) BridgeOut.encodeText(response, "skinMessage", skinStatus);

        saveEntry(uuidEntry);
        saveEntry(ipEntry);
        saveEntry(nameEntry);

        response.addProperty("success", true);
        return response;
    }

    private static Text canPlayerJoin(PlayerEntry uuidEntry, PlayerEntry ipEntry, PlayerEntry nameEntry, Instant now, String whitelistStatus) {
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

    public static void tickPlayerMutes(JsonArray arr) {
        Instant now = Instant.now();
        for (JsonElement element : arr) {
            String uuid = element.getAsString();
            PlayerEntry entry = loadUUID(uuid);

            if (entry.isActiveMute(now) == null) {
                MuteAction.muteExpire("", entry);
                saveEntry(entry);
            }
        }
    }


    private static Text checkPlayerSkinBans(PlayerEntry entry, String skin) {
        SkinBanEntry banEntry = entry.isActiveSkinBan(skin);
        if (banEntry != null) {
            return Text.translatable("gui.banned.skin.title").formatted(Formatting.BOLD, Formatting.RED).append("\n\n")
                    .append(Text.translatableWithFallback("punishment.reason", "Reason: %s", Text.literal(banEntry.banSource.why).formatted(Formatting.YELLOW)));
        }
        return null;
    }

    private static Text checkPlayerEntryBans(PlayerEntry entry, String whitelistStatus, Instant now) {
        BanEntry banEntry = entry.isActiveBan(now);
        if (banEntry != null) {
            return BanAction.getBanText(banEntry, now);
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

    public static void multiRevoke(String id_types, String uuid, String who, String source, String reason) {
        if (id_types.contains("_b_")) BanAction.revokeUUIDBans(uuid, who, source, reason);
        if (id_types.contains("_m_")) MuteAction.revokeMutes(uuid, who, source, reason);
        if (id_types.contains("_sb_")) SkinBanAction.revokeSkinBan(uuid, who, source, reason);
        if (id_types.contains("_wl_")) WhitelistAction.revokeWhitelist(uuid, who, source, reason);
        if (id_types.contains("_ss_")) SusAction.revokeSus(uuid, who, source, reason);
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
        LinkingManager.load(readFileJson(gson, "discordLinks.json", JsonObject.class));
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


    public static Path getSkinRenderPath(String url) {
        return LEMBackend.ProhibitorModule.module.savePath.resolve("SKINRENDERS").resolve(url.substring(38) + ".png");
    }

    public static void notifyServer(String source, Actions action, PlayerEntry entry, Entry banEntry, Text msg) {
        notifyServer(source, action, entry, banEntry, msg, null);
    }

    public static void notifyServer(String source, Actions action, PlayerEntry entry, Entry banEntry, Text msg, JsonObject custom) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", entry.associatedUUID);
        obj.addProperty("action", action.name());
        BridgeOut.encodeText(obj, "reason", msg);
        if (custom != null) obj.add("custom", custom);
        if (action == Actions.MUTE) obj.addProperty("muteDuration", ((BanEntry) banEntry).getRemaining(Instant.now()));
        BridgeOut.sendMessageToAllServers("prohibitor", obj);

        List<ContainerChildComponent> container = new ArrayList<>();
        container.add(ProhibitorDiscordCommands.getTitle("Punishment Issued"));
        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        ViewCommand.buildPunishment(container, action, banEntry);
        container.add(3, Separator.createInvisible(Separator.Spacing.SMALL));
        container.add(4, TextDisplay.of("**Player:** " + entry.associatedName + " (" + entry.associatedUUID + ")"));
        container.add(5, TextDisplay.of("**Source:** " + source));
        BridgeModule.adminLogWebhook.sendMessageComponents(Container.of(container)).useComponentsV2().queue();
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
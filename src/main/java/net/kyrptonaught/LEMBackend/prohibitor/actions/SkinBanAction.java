package net.kyrptonaught.LEMBackend.prohibitor.actions;

import com.google.common.collect.Iterables;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.util.UndashedUuid;
import net.kyrptonaught.LEMBackend.FileHelper;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.SkinBanEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.LenientJsonParser;

import java.time.Instant;
import java.util.Base64;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class SkinBanAction {
    public static boolean skinBan(String uuid, String who, String source, String reason, String... evidence) {
        String url = getPlayerSkin(uuid);
        if (url == null) return false;
        generateSkinRender(url);
        skinBan(uuid, url, who, source, reason, evidence);
        return true;
    }

    public static void skinBan(String uuid, String skin, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        SkinBanEntry actionEntry = new SkinBanEntry(skin, who, source, reason).addEvidence(evidence);
        entry.skinBans.add(actionEntry);
        generateSkinRender(skin);

        saveEntry(entry);
        ProhibitorModule.notifyServer(source, Actions.SKINBAN, entry, actionEntry, getBanText(actionEntry));
    }


    public static void revokeSkinBan(String uuid, String who, String source, String reason) {
        PlayerEntry entry = loadUUID(uuid);
        entry.skinBans.clear();
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, Actions.UNSKINBAN, entry, new StampEntry(who, source, Instant.now(), reason), Text.empty());
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
        FileHelper.download(api, ProhibitorModule.getSkinRenderPath(url));
    }

    public static Text getBanText(SkinBanEntry banEntry) {
        if (banEntry != null) {
            return Text.translatable("gui.banned.skin.title").formatted(Formatting.BOLD, Formatting.RED).append("\n\n")
                    .append(Text.translatableWithFallback("punishment.reason", "Reason: %s", Text.literal(banEntry.banSource.why).formatted(Formatting.YELLOW)));
        }
        return null;
    }
}

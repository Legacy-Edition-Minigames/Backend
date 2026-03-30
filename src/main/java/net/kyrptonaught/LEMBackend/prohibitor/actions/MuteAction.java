package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.BanEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.StampEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class MuteAction {

    public static void permaMute(String uuid, String who, String source, String reason, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        BanEntry banEntry = BanEntry.PermaBan(reason, who, source).addEvidence(evidence);
        entry.mutes.addFirst(banEntry);
        ProhibitorModule.notifyServer(source, Actions.MUTE, entry, banEntry, getMuteText(banEntry, Instant.now()));
        saveEntry(entry);
    }

    public static void tempMute(String uuid, String who, String source, String reason, int duration_time, byte duration_type, String... evidence) {
        PlayerEntry entry = loadUUID(uuid);
        BanEntry banEntry = BanEntry.TempBan(reason, duration_time, duration_type, who, source).addEvidence(evidence);
        entry.mutes.addFirst(banEntry);
        ProhibitorModule.notifyServer(source, Actions.MUTE, entry, banEntry, getMuteText(banEntry, Instant.now()));
        saveEntry(entry);
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
        ProhibitorModule.notifyServer(source, Actions.UNMUTE, entry, new StampEntry(who, source, now, reason), Text.translatableWithFallback("prohibitor.mute.unmuted", "You have been unmuted"));
    }

    public static void muteExpire(String source, PlayerEntry entry) {
        ProhibitorModule.notifyServer(source, Actions.UNMUTE, entry, new StampEntry("Expired", source, Instant.now(), "Expired"), getUnMuteText());
    }

    public static Text getMuteText(BanEntry banEntry, Instant now) {
        if (banEntry != null) {
            return Text.translatableWithFallback("prohibitor.mute.cannotsent", "You are muted, your messages will not be sent").formatted(Formatting.BOLD, Formatting.RED).append("\n")
                    .append(Text.translatableWithFallback("punishment.reason", "Reason: %s", Text.literal(banEntry.banSource.why).formatted(Formatting.YELLOW))).append("\n")
                    .append(Text.translatableWithFallback("punishment.expires", "Expires in: %s", banEntry.getDurationText(now).formatted(Formatting.YELLOW)));
        }
        return null;
    }

    public static Text getStillMuteText(BanEntry banEntry, Instant now) {
        if (banEntry != null) {
            return Text.translatableWithFallback("prohibitor.mute.cannotsent", "You are muted, your messages will not be sent").formatted(Formatting.RED).append("\n")
                    .append(Text.translatableWithFallback("punishment.expires", "Expires in: %s", banEntry.getDurationText(now).formatted(Formatting.YELLOW)));
        }
        return null;
    }

    public static Text getUnMuteText() {
        return Text.translatableWithFallback("prohibitor.mute.unmuted", "You have been unmuted").formatted(Formatting.YELLOW).append("\n")
                .append(Text.translatableWithFallback("punishment.reason", "Reason: %s", Text.translatable("mco.configure.world.subscription.expired").formatted(Formatting.YELLOW)));
    }
}

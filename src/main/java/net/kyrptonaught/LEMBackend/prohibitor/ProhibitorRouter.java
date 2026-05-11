package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.serialization.JsonOps;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.ModuleRouter;
import net.kyrptonaught.LEMBackend.prohibitor.actions.*;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.LenientJsonParser;

import java.util.Base64;

public class ProhibitorRouter extends ModuleRouter<ProhibitorModule> {

    @Override
    public ProhibitorModule createModule() {
        return new ProhibitorModule();
    }

    @Override
    public void addRoutes(RoutesConfig routes) {
        route(routes, HTTP.GET, "/v1/{secret}/prohibitor/joincheck/{whiteliststatus}", this::checkPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/ban/perm/{uuid}", this::permBanPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/ban/temp/{uuid}", this::tempBanPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/ban/skin/{uuid}", this::skinBanPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/unban/uuid/{uuid}", this::unBanUUIDPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/unban/name/{name}", this::unBanNAMEPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/unban/ip/{ip}", this::unBanIPPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/mute/temp/{uuid}", this::tempMutePlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/mute/perm/{uuid}", this::permMutePlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/unmute/{uuid}", this::unMutePlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/kick/{uuid}", this::kickPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/warn/{uuid}", this::warnPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/whitelist/add/{uuid}", this::whitelistPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/whitelist/remove/{uuid}", this::unwhitelistPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/mute/tick", this::tickMutes);

        route(routes, HTTP.GET, "/v1/{secret}/prohibitor/chatfilter/get", this::getChatfilter);

        route(routes, HTTP.POST, "/v1/{secret}/link/start/{linkid}/{mcuuid}/{server}", this::startLink);

        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/sus/add/{uuid}", this::susPlayer);
        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/sus/remove/{uuid}", this::unSusPlayer);

        route(routes, HTTP.POST, "/v1/{secret}/prohibitor/report/{uuid}", this::reportPlayer);
    }

    private void checkPlayer(Context ctx) {
        String whitelistStatus = ctx.pathParam("whiteliststatus");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        GameProfile profile = ExtraCodecs.AUTHLIB_GAME_PROFILE.parse(JsonOps.INSTANCE, obj.get("profile")).result().get();
        String ip = obj.get("ip").getAsString();

        String skin = null;
        Property prop = Iterables.getFirst(profile.properties().get("textures"), null);
        if (prop != null) skin = LenientJsonParser.parse(new String(Base64.getDecoder().decode(prop.value()))).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

        JsonObject response = ProhibitorModule.getJoinStatus(profile.id().toString(), profile.name(), ip, skin, whitelistStatus, true);
        ctx.result(response.toString());
    }

    private void getChatfilter(Context ctx) {
        ctx.result(ChatFilter.json().toString());
    }

    private void tickMutes(Context ctx) {
        JsonArray obj = ctx.bodyAsClass(JsonArray.class);
        ProhibitorModule.tickPlayerMutes(obj);
        ctx.result("success");
    }

    private void permBanPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);
        BanAction.multiPermBan("_uuid_" + "_ip_", uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void tempBanPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        BanAction.multiTempBan("_uuid_" + "_ip_", uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString(), obj.get("duration_time").getAsInt(), obj.get("duration_type").getAsByte());
    }

    private void skinBanPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        SkinBanAction.skinBan(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void unBanUUIDPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        BanAction.revokeUUIDBans(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void unBanIPPlayer(Context ctx) {
        String ip = ctx.pathParam("ip");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        BanAction.revokeIPBans(ip, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void unBanNAMEPlayer(Context ctx) {
        String name = ctx.pathParam("name");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        BanAction.revokeNAMEBans(name, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void permMutePlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        MuteAction.permaMute(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void tempMutePlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        MuteAction.tempMute(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString(), obj.get("duration_time").getAsInt(), obj.get("duration_type").getAsByte());
    }

    private void unMutePlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        MuteAction.revokeMutes(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void kickPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        KickAction.kick(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void warnPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        WarnAction.warn(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void whitelistPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ctx.result("" + WhitelistAction.whitelist(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString()));
    }

    private void unwhitelistPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        WhitelistAction.revokeWhitelist(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void susPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ctx.result("" + SusAction.sus(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString()));
    }

    private void unSusPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);
        SusAction.revokeSus(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void reportPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ReportAction.report(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    public void startLink(Context ctx) {
        String linkID = ctx.pathParam("linkid");
        String mcUUID = ctx.pathParam("mcuuid");
        String server = ctx.pathParam("server");

        LinkingManager.startLink(linkID, mcUUID, server);
        ctx.result("success");
    }
}
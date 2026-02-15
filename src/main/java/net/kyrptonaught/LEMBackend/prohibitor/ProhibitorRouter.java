package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.serialization.JsonOps;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.ModuleRouter;
import net.kyrptonaught.LEMBackend.prohibitor.entries.ID_TYPE;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.dynamic.Codecs;

import java.util.Base64;

public class ProhibitorRouter extends ModuleRouter<ProhibitorModule> {

    @Override
    public ProhibitorModule createModule() {
        return new ProhibitorModule();
    }

    @Override
    public void addRoutes() {
        route(HTTP.GET, "/v0/{secret}/prohibitor/joincheck/{whiteliststatus}", this::checkPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/ban/perm/{uuid}", this::permBanPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/mute/perm/{uuid}", this::permMutePlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/ban/temp/{uuid}", this::tempBanPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/mute/temp/{uuid}", this::tempMutePlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/ban/skin/{uuid}", this::skinBanPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/kick/{uuid}", this::kickPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/warn/{uuid}", this::warnPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/whitelist/add/{uuid}", this::whitelistPlayer);

        route(HTTP.POST, "/v0/{secret}/link/start/{linkid}/{mcuuid}/{server}", this::startLink);

        route(HTTP.POST, "/v0/{secret}/prohibitor/sus/add/{uuid}", this::susPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/sus/remove/{uuid}", this::unSusPlayer);
    }

    private void checkPlayer(Context ctx) {
        String whitelistStatus = ctx.pathParam("whiteliststatus");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        GameProfile profile = Codecs.GAME_PROFILE_CODEC.parse(JsonOps.INSTANCE, obj.get("profile")).result().get();
        String ip = obj.get("ip").getAsString();

        String skin = null;
        Property prop = Iterables.getFirst(profile.properties().get("textures"), null);
        if (prop != null) skin = LenientJsonParser.parse(new String(Base64.getDecoder().decode(prop.value()))).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

        JsonObject response = module.getJoinStatus(profile.id().toString(), profile.name(), ip, skin, whitelistStatus);
        ctx.result(response.toString());
    }

    private void permBanPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);
        ProhibitorModule.multiPermBan("_uuid_" + "_ip_", uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void permMutePlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ProhibitorExecuter.permaMute(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void tempBanPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ProhibitorModule.multiTempBan("_uuid_" + "_ip_", uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString(), obj.get("duration_time").getAsInt(), obj.get("duration_type").getAsByte());
    }

    private void tempMutePlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ProhibitorExecuter.tempMute(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString(), obj.get("duration_time").getAsInt(), obj.get("duration_type").getAsByte());
    }

    private void skinBanPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ProhibitorModule.skinBan(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void kickPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ProhibitorExecuter.kick(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void warnPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ProhibitorExecuter.warn(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void whitelistPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ctx.result("" + ProhibitorExecuter.whitelist(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString()));
    }

    private void susPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ctx.result("" + ProhibitorExecuter.sus(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString()));
    }

    private void unSusPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);
        ProhibitorExecuter.revokeSus(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    public void startLink(Context ctx) {
        String linkID = ctx.pathParam("linkid");
        String mcUUID = ctx.pathParam("mcuuid");
        String server = ctx.pathParam("server");

        LinkingManager.startLink(linkID, mcUUID, server);
        ctx.result("success");
    }
}
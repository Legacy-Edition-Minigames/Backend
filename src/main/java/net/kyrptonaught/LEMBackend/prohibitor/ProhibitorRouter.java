package net.kyrptonaught.LEMBackend.prohibitor;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.javalin.http.Context;
import net.kyrptonaught.LEMBackend.ModuleRouter;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class ProhibitorRouter extends ModuleRouter<ProhibitorModule> {

    @Override
    public ProhibitorModule createModule() {
        return new ProhibitorModule();
    }

    @Override
    public void addRoutes() {
        route(HTTP.GET, "/v0/{secret}/prohibitor/joincheck/{whiteliststatus}/{uuid}", this::checkPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/ban/perm/{uuid}", this::permBanPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/mute/perm/{uuid}", this::permMutePlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/ban/temp/{uuid}", this::tempBanPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/mute/temp/{uuid}", this::tempMutePlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/kick/{uuid}", this::kickPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/warn/{uuid}", this::warnPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/whitelist/add/{uuid}", this::whitelistPlayer);

        route(HTTP.POST, "/v0/{secret}/link/start/{linkid}/{mcuuid}/{server}", this::startLink);

        route(HTTP.POST, "/v0/{secret}/prohibitor/sus/add/{uuid}", this::susPlayer);
        route(HTTP.POST, "/v0/{secret}/prohibitor/sus/remove/{uuid}", this::unSusPlayer);
    }

    private void checkPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        String whitelistStatus = ctx.pathParam("whiteliststatus");

        Text response = module.canPlayerJoin(uuid, whitelistStatus);
        if (response != null) ctx.result(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, response).getOrThrow().toString());
        else ctx.result("success");
    }

    private void permBanPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        module.permaBan(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void permMutePlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        module.permaMute(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void tempBanPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        module.tempBan(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString(), obj.get("duration_time").getAsInt(), obj.get("duration_type").getAsByte());
    }

    private void tempMutePlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        module.tempMute(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString(), obj.get("duration_time").getAsInt(), obj.get("duration_type").getAsByte());
    }

    private void kickPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        module.kick(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void warnPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        module.warn(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString());
    }

    private void whitelistPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ctx.result("" + module.whitelist(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString()));
    }

    private void susPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        JsonObject obj = ctx.bodyAsClass(JsonObject.class);

        ctx.result("" + module.sus(uuid, obj.get("stamp_who").getAsString(), obj.get("stamp_source").getAsString(), obj.get("stamp_reason").getAsString()));
    }

    private void unSusPlayer(Context ctx) {
        String uuid = ctx.pathParam("uuid");
        module.unSus(uuid);
    }

    public void startLink(Context ctx) {
        String linkID = ctx.pathParam("linkid");
        String mcUUID = ctx.pathParam("mcuuid");
        String server = ctx.pathParam("server");

        LinkingManager.startLink(linkID, mcUUID, server);
        ctx.result("success");
    }
}
package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonObject;
import io.javalin.websocket.WsContext;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

public class ServerInfo {
    public String serverName;

    public final WsContext socketConnection;
    public final long chatChannelID;
    public final Webhook chatChannelWebhook;

    public final long logChannelID;
    public final Webhook logChannelWebhook;

    public IReplyCallback infoCommandInteraction;
    public IReplyCallback personatusStatusInteraction;

    public ServerInfo(WsContext socketConnection, long chatChannelID, Webhook chatChannelWebhook, long logChannelID, Webhook logChannelWebhook) {
        this.socketConnection = socketConnection;
        this.chatChannelID = chatChannelID;
        this.chatChannelWebhook = chatChannelWebhook;
        this.logChannelID = logChannelID;
        this.logChannelWebhook = logChannelWebhook;
    }

    public void send(String obj) {
        socketConnection.send(obj);
    }

    public void send(JsonObject obj) {
        send(obj.toString());
    }

    public void requestInfoCommand(IReplyCallback event) {
        this.infoCommandInteraction = event;
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "info_request");
        send(obj);
    }

    public void requestPersonatusStatusCommand(IReplyCallback event) {
        this.personatusStatusInteraction = event;
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "personatus_request");
        send(obj);
    }
}

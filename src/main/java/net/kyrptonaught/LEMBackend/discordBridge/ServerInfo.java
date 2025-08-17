package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonObject;
import io.javalin.websocket.WsContext;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

public class ServerInfo {
    public WsContext socketConnection;
    public long discordChannelID;
    public String discordChannelWebhook;

    public SlashCommandInteraction infoCommandInteraction;

    public ServerInfo(WsContext socketConnection, long discordChannelID, String discordChannelWebhook) {
        this.socketConnection = socketConnection;
        this.discordChannelID = discordChannelID;
        this.discordChannelWebhook = discordChannelWebhook;
    }

    public void send(String obj) {
        socketConnection.send(obj);
    }

    public void send(JsonObject obj) {
        send(obj.toString());
    }

    public void requestInfoCommand(SlashCommandInteraction event) {
        this.infoCommandInteraction = event;
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "info_request");
        send(obj);
    }
}

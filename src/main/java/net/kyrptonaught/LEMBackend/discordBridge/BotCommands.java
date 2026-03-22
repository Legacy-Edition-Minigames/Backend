package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.discordBridge.format.FormatToDiscord;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorDiscordCommands;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;
import net.kyrptonaught.LEMBackend.userConfig.UserConfigDiscordCommands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.List;

public class BotCommands {

    public static void registerCommands(JDA jda) {
        jda.updateCommands().addCommands(Commands.slash("info", "Get the server info")).addCommands(ProhibitorDiscordCommands.register()).addCommands(UserConfigDiscordCommands.register()).queue();
    }

    public static void execute(JDA jda, SlashCommandInteraction event) {
        MinecraftServer server = LEMBackend.minecraftServer;
        switch (event.getName()) {
            case "info" -> infoCommandExecute(server, event);
            case "prohibitor" -> ProhibitorDiscordCommands.execute(event);
            case "userconfig", "legacyuserconfig" -> UserConfigDiscordCommands.execute(event);
        }
    }

    public static void buttonPressed(JDA jda, ButtonInteractionEvent event) {
        if (event.getCustomId().startsWith("prohibitor"))
            ProhibitorDiscordCommands.buttonInteraction(event);
        if (event.getButton().getCustomId().equals("link:start")) {
            LinkingManager.displayLinkInput(event);
        }
    }

    public static void modalInteraction(JDA jda, ModalInteractionEvent event) {
        if (event.getCustomId().startsWith("prohibitor"))
            ProhibitorDiscordCommands.modalSubmit(event);
        if (event.getModalId().equals("link:modal"))
            LinkingManager.linkInputResults(event);
        if (event.getCustomId().startsWith("userconfig"))
            UserConfigDiscordCommands.modalSubmit(event);
    }

    public static void selectInteraction(JDA jda, StringSelectInteractionEvent event) {
        if (event.getCustomId().startsWith("prohibitor"))
            ProhibitorDiscordCommands.selectInteraction(event);
    }

    public static void messageContextInteraction(JDA jda, MessageContextInteractionEvent event) {
        if (event.getCommandString().startsWith("Prohibitor"))
            ProhibitorDiscordCommands.messageContextInteraction(event);
        if (event.getCommandString().startsWith("UserConfig"))
            UserConfigDiscordCommands.messageContextInteraction(event);
    }

    public static void infoCommandExecute(MinecraftServer server, IReplyCallback event) {
        if (BridgeModule.servers.containsKey(event.getChannel().getName())) {
            event.deferReply().queue();
            BridgeModule.servers.get(event.getChannel().getName()).requestInfoCommand(event);
        }
    }

    public static void infoCommandResponse(JsonObject obj, IReplyCallback event) {
        double serverTickTime = average(obj.get("server_tick_times").getAsJsonArray()) * 1.0E-6D;
        long total_memory = obj.get("total_memory").getAsLong();
        long free_memory = obj.get("free_memory").getAsLong();

        int playerCount = 0;
        int spectatorCount = 0;

        StringBuilder playerString = new StringBuilder("```css\n");
        StringBuilder spectatorString = new StringBuilder("```css\n");
        JsonArray players = obj.getAsJsonArray("players");

        for (JsonElement player : players) {
            if (player.getAsJsonObject().get("isSpectator").getAsBoolean()) {
                spectatorCount++;
                spectatorString.append("[").append(player.getAsJsonObject().get("latency").getAsInt()).append("ms] ").append(player.getAsJsonObject().get("name").getAsString()).append("\n");
            } else {
                playerCount++;
                playerString.append("[").append(player.getAsJsonObject().get("latency").getAsInt()).append("ms] ").append(player.getAsJsonObject().get("name").getAsString()).append("\n");
            }
        }
        if (playerCount == 0) playerString.append("None");
        if (spectatorCount == 0) spectatorString.append("None");
        playerString.append("```");
        spectatorString.append("```");

        event.getHook().editOriginalEmbeds(
                new EmbedBuilder()
                        .setTitle("Server Status")
                        .setColor(0x00aaff)
                        .addField("Players (" + playerCount + "/" + obj.get("player_count").getAsInt() + ")", playerString.toString(), true)
                        .addField("Spectators (" + spectatorCount + "/" + obj.get("spectator_count").getAsInt() + ")", spectatorString.toString(), true)
                        .addField("",
                                "- **TPS**: " + String.format("%.2f", Math.min(1000.0 / serverTickTime, 20)) +
                                        "\n- **MSPT**: " + String.format("%.2f", serverTickTime) +
                                        "\n- **RAM**: " + (100 - (int) ((double) free_memory / total_memory * 100)) + "%"
                                , false)
                        .build()).queue();
    }

    public static void gameStartInfo(JDA jda, long channelID, JsonObject obj) {
        String mapName = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, TextCodecs.CODEC.parse(JsonOps.INSTANCE, obj.get("map_name")).result().get(), true).replaceAll("!", "");
        List<ContainerChildComponent> container = new ArrayList<>();
        container.add(Section.of(Button.secondary("empty", obj.get("game_id").getAsString()).asDisabled(), TextDisplay.of("# " + mapName), TextDisplay.of("-# " + obj.get("map_size").getAsString())));
        //container.add(TextDisplay.ofFormat("# %s\n-# %s", mapName, obj.get("map_size").getAsString()));
        container.add(MediaGallery.of(MediaGalleryItem.fromUrl("https://raw.githubusercontent.com/Team-Lodestone/Documentation/refs/heads/main/LCE/Game/BattleMapImages/" + mapName + ".png")));
        container.add(TextDisplay.ofFormat("> Players: %s\n> Spectators: %s", obj.get("player_count").getAsString(), obj.get("spectator_count").getAsString()));
        container.add(Separator.createDivider(Separator.Spacing.SMALL));

        JsonArray rules = obj.getAsJsonArray("changed_rules");
        if (rules.isEmpty()) {
            container.add(TextDisplay.of("### Default Rules"));
        } else {
            StringBuilder str = new StringBuilder("### Changed Rules:");
            for (JsonElement entry : rules.asList()) {
                String key = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, TextCodecs.CODEC.parse(JsonOps.INSTANCE, entry.getAsJsonObject().get("key")).result().get(), true);
                String value = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, TextCodecs.CODEC.parse(JsonOps.INSTANCE, entry.getAsJsonObject().get("value")).result().get(), true);
                str.append("\n- **" + key + "**: " + value);
            }
            container.add(TextDisplay.of(str.toString()));
        }

        jda.getTextChannelById(channelID).sendMessage("Starting Game:").queue();
        jda.getTextChannelById(channelID).sendMessageComponents(Container.of(container)).useComponentsV2().queue();
    }

    private static double average(JsonArray array) {
        long l = 0L;
        for (JsonElement jsonElement : array) {
            l += jsonElement.getAsLong();
        }
        return (double) l / (double) array.size();
    }
}

package net.kyrptonaught.LEMBackend.discordBridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.kyrptonaught.LEMBackend.IO;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.discordBridge.format.FormatToDiscord;
import net.kyrptonaught.LEMBackend.discordBridge.linking.LinkingManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TextCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BotCommands {

    public static void registerCommands(JDA jda) {
        jda.updateCommands().addCommands(
                Commands.slash("info", "Get the server info"),
                Commands.slash("sus", "Mark a player as suspicious").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)).addOption(OptionType.STRING, "mcname", "MC Username"),
                Commands.slash("unsus", "Mark a player as no longer suspicious").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)).addOption(OptionType.STRING, "mcname", "MC Username")
        ).queue();
    }

    public static void execute(JDA jda, SlashCommandInteraction event) {
        MinecraftServer server = LEMBackend.minecraftServer;
        switch (event.getName()) {
            case "info" -> infoCommandExecute(server, event);
            case "sus" -> {
                event.deferReply().queue();
                String mcname = event.getOption("mcname").getAsString();
                susCommandExecute(mcname, (result) -> event.getHook().editOriginal(result).queue());
            }
            case "unsus" -> {
                event.deferReply().queue();
                String mcname = event.getOption("mcname").getAsString();
                unsusCommandExecute(mcname, (result) -> event.getHook().editOriginal(result).queue());
            }
        }
    }

    public static void buttonPressed(JDA jda, @NotNull ButtonInteractionEvent event) {
        if (event.getButton().getCustomId().equals("link:start")) {
            LinkingManager.displayLinkInput(event);
        }
    }

    public static void modalInteraction(JDA jda, ModalInteractionEvent event) {
        if (event.getModalId().equals("link:modal")) {
            LinkingManager.linkInputResults(event);
        }
    }

    public static void infoCommandExecute(MinecraftServer server, SlashCommandInteraction event) {
        if (BridgeModule.servers.containsKey(event.getChannel().getName())) {
            event.deferReply().queue();
            BridgeModule.servers.get(event.getChannel().getName()).requestInfoCommand(event);
        }
    }

    public static void infoCommandResponse(JsonObject obj, SlashCommandInteraction event) {
        double serverTickTime = average(obj.get("server_tick_times").getAsJsonArray()) * 1.0E-6D;
        long total_memory = obj.get("total_memory").getAsLong();
        long free_memory = obj.get("free_memory").getAsLong();
        long used_ram = (total_memory - free_memory) / 1024 / 1024;

        StringBuilder playerString = new StringBuilder();
        JsonArray players = obj.getAsJsonArray("players");
        playerString.append("```css\n").append("Online Players (").append(players.size()).append("/").append(obj.get("max_players").getAsInt()).append(")\n");
        for (JsonElement player : players)
            playerString.append("[").append(player.getAsJsonObject().get("latency").getAsInt()).append("ms] ").append(player.getAsJsonObject().get("name").getAsString()).append("\n");
        playerString.append("```");

        event.getHook().editOriginalEmbeds(
                new EmbedBuilder()
                        .setTitle("Server Status")
                        .setDescription(playerString.toString())
                        .setColor(0x00aaff)
                        .addField("TPS", String.format("%.2f", Math.min(1000.0 / serverTickTime, 20)), true)
                        .addField("MSPT", String.format("%.2f", serverTickTime), true)
                        .addField("RAM", used_ram + "MB / " + total_memory / 1024 / 1024 + "MB", true)
                        .build()).queue();
    }

    public static void gameStartInfo(JDA jda, long channelID, JsonObject obj) {
        String mapName = FormatToDiscord.toDiscord(jda, LEMBackend.minecraftServer, TextCodecs.CODEC.parse(JsonOps.INSTANCE, obj.get("map_name")).result().get(), true);
        List<ContainerChildComponent> container = new ArrayList<>();

        container.add(TextDisplay.ofFormat("# %s\n-# %s", mapName, obj.get("map_size").getAsString()));
        container.add(MediaGallery.of(MediaGalleryItem.fromUrl("https://raw.githubusercontent.com/Team-Lodestone/Documentation/refs/heads/main/LCE/Game/BattleMapImages/" + mapName + ".png")));
        container.add(TextDisplay.ofFormat("> Players: %s/16\n> Spectators: %s/16", obj.get("player_count").getAsString(), obj.get("spectator_count").getAsString()));
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

    public static void susCommandExecute(String mcName, Consumer<String> result) {
        String responseUUID = IO.getValue("https://api.mojang.com/users/profiles/minecraft/" + mcName, "id");

        if (responseUUID == null) {
            result.accept("Invalid MC Name");
            return;
        }

        LEMBackend.LinkingModule.module.addSus(responseUUID);
        result.accept("Added **" + mcName + "** as a suspicious player");
        WebhookSender.log(BridgeModule.config.loggingWebhookURL, "Suspicious", "Added **" + mcName + "** as a suspicious player");
    }

    public static void unsusCommandExecute(String mcName, Consumer<String> result) {
        String responseUUID = IO.getValue("https://api.mojang.com/users/profiles/minecraft/" + mcName, "id");

        if (responseUUID == null) {
            result.accept("Invalid MC Name");
            return;
        }
        LEMBackend.LinkingModule.module.removeSus(responseUUID);
        result.accept("Removed **" + mcName + "** as a suspicious player");
        WebhookSender.log(BridgeModule.config.loggingWebhookURL, "Suspicious", "Removed **" + mcName + "** as a suspicious player");
    }

    private static double average(JsonArray array) {
        long l = 0L;
        for (JsonElement jsonElement : array) {
            l += jsonElement.getAsLong();
        }
        return (double) l / (double) array.size();
    }

}

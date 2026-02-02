package net.kyrptonaught.LEMBackend.prohibitor;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.modals.Modal;
import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.prohibitor.linking.LinkingManager;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ProhibitorDiscordCommands {

    public static SlashCommandData register() {
        return Commands.slash("prohibitor", "Punish Players").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS));
    }

    public static void execute(SlashCommandInteraction event) {
        event.deferReply().queue();
        event.getHook().sendMessageComponents(Container.of(buildMessage("prohibitor:player_name"))).useComponentsV2().queue();
    }


    public static void selectInteraction(StringSelectInteractionEvent event) {
        String menu = event.getComponent().getCustomId();
        if (menu.equals("prohibitor:player_id_type")) event.editComponents(Container.of(buildMessage(event.getInteraction().getValues().getFirst()))).useComponentsV2().queue();
    }

    public static void buttonInteraction(ButtonInteractionEvent event) {
        String button = event.getButton().getCustomId();
        if (button.startsWith("prohibitor:button_")) event.replyModal(buildModal(button)).queue();
    }

    public static void modalSubmit(ModalInteractionEvent event) {
        String id = event.getCustomId();

        String player_uuid = null;

        if (id.contains("player_uuid")) {
            player_uuid = event.getValue("prohibitor:player_id").getAsString();
        } else if (id.contains("player_name")) {
            player_uuid = ProhibitorModule.getUUIDFromName(event.getValue("prohibitor:player_id").getAsString());
        } else if (id.contains("player_discord")) {
            long discord = event.getValue("prohibitor:player_id").getAsLongList().getFirst();
            player_uuid = LinkingManager.getMCFromDiscord(discord);
        }

        String who = event.getMember().getEffectiveName() + " - Discord";
        String source = event.getGuild().getName() + " - Discord";
        String reason = event.getValue("prohibitor:reason").getAsString();

        if (id.contains("_ban_")) {
            if (id.contains("_perm_")) LEMBackend.ProhibitorModule.module.permaBan(player_uuid, who, source, reason);
            else if (id.contains("_temp_")) {
                int durationTime = Integer.parseInt(event.getValue("prohibitor:duration_time").getAsString());
                byte durationType = Byte.parseByte(event.getValue("prohibitor:duration_type").getAsStringList().getFirst().split("_")[2]);
                LEMBackend.ProhibitorModule.module.tempBan(player_uuid, who, source, reason, durationTime, durationType);
            }
        } else if (id.contains("_mute_")) {
            if (id.contains("_perm_")) LEMBackend.ProhibitorModule.module.permaMute(player_uuid, who, source, reason);
            else if (id.contains("_temp_")) {
                int durationTime = Integer.parseInt(event.getValue("prohibitor:duration_time").getAsString());
                byte durationType = Byte.parseByte(event.getValue("prohibitor:duration_type").getAsStringList().getFirst().split("_")[2]);
                LEMBackend.ProhibitorModule.module.tempMute(player_uuid, who, source, reason, durationTime, durationType);
            }
        } else if (id.contains("_kick_")) LEMBackend.ProhibitorModule.module.kick(player_uuid, who, source, reason);
        else if (id.contains("warn")) LEMBackend.ProhibitorModule.module.warn(player_uuid, who, source, reason);
        else if (id.contains("sus")) LEMBackend.ProhibitorModule.module.sus(player_uuid, who, source, reason);
        else if (id.contains("whitelist")) LEMBackend.ProhibitorModule.module.whitelist(player_uuid, who, source, reason);

        event.reply("Success!").setEphemeral(true).queue();
    }

    private static List<ContainerChildComponent> buildMessage(String playerBanDefault) {
        List<ContainerChildComponent> container = new ArrayList<>();

        container.add(Section.of(Thumbnail.fromUrl("https://assets.mcasset.cloud/1.21.11/assets/minecraft/textures/item/mace.png"), TextDisplay.of("## Prohibitor")));
        container.add(Separator.createInvisible(Separator.Spacing.LARGE));

        container.add(TextDisplay.of("Player ID Type:"));
        container.add(ActionRow.of(StringSelectMenu.create("prohibitor:player_id_type")
                .addOption("MC Name", "prohibitor:player_name")
                .addOption("MC UUID", "prohibitor:player_uuid")
                .addOption("Discord", "prohibitor:player_discord")
                .setPlaceholder("Player ID Type")
                .setDefaultValues(playerBanDefault)
                .setRequired(true).build()));
        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        container.add(TextDisplay.of("Bans"));
        container.add(ActionRow.of(Button.primary("prohibitor:button_ban_perm_" + playerBanDefault, "Perm Ban"), Button.primary("prohibitor:button_ban_temp_" + playerBanDefault, "Temp Ban")));
        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        container.add(TextDisplay.of("Mutes"));
        container.add(ActionRow.of(Button.primary("prohibitor:button_mute_perm_" + playerBanDefault, "Perm Mute"), Button.primary("prohibitor:button_mute_temp_" + playerBanDefault, "Temp Mute")));
        container.add(Separator.createDivider(Separator.Spacing.LARGE));

        container.add(TextDisplay.of("Other"));
        container.add(ActionRow.of(Button.primary("prohibitor:button_whitelist_" + playerBanDefault, "Whitelist"), Button.primary("prohibitor:button_warn_" + playerBanDefault, "Warning"), Button.primary("prohibitor:button_kick_" + playerBanDefault, "Kick"), Button.primary("prohibitor:button_sus_" + playerBanDefault, "Sus")));

        return container;
    }

    private static Modal buildModal(String button) {
        List<ModalTopLevelComponent> container = new ArrayList<>();

        if (button.endsWith("player_name"))
            container.add(Label.of("Player's MC Name: ", TextInput.create("prohibitor:player_id", TextInputStyle.SHORT).setPlaceholder("MC Name").setRequired(true).build()));
        else if (button.endsWith("player_uuid"))
            container.add(Label.of("Player's MC UUID: ", TextInput.create("prohibitor:player_id", TextInputStyle.SHORT).setPlaceholder("MC UUID").setRequired(true).build()));
        else if (button.endsWith("player_discord"))
            container.add(Label.of("Player's Discord: ", EntitySelectMenu.create("prohibitor:player_id", EntitySelectMenu.SelectTarget.USER).setRequired(true).build()));

        if (button.contains("_temp_")) {
            container.add(Label.of("Duration: ", TextInput.create("prohibitor:duration_time", TextInputStyle.SHORT).setPlaceholder("Duration").setRequired(true).build()));
            container.add(Label.of("Unit: ", StringSelectMenu.create("prohibitor:duration_type")
                    .addOption("Second(s)", "prohibitor:duration_second_" + ChronoUnit.SECONDS.ordinal())
                    .addOption("Minute(s)", "prohibitor:duration_minute_" + ChronoUnit.MINUTES.ordinal())
                    .addOption("Hour(s)", "prohibitor:duration_hour_" + ChronoUnit.HOURS.ordinal())
                    .addOption("Day(s)", "prohibitor:duration_day_" + ChronoUnit.DAYS.ordinal())
                    .addOption("Month(s)", "prohibitor:duration_month_" + ChronoUnit.MONTHS.ordinal())
                    .addOption("Year(s)", "prohibitor:duration_year_" + ChronoUnit.YEARS.ordinal())
                    .setPlaceholder("Duration").setRequired(true).build()));
        }

        container.add(Label.of("Reason: ", TextInput.create("prohibitor:reason", TextInputStyle.PARAGRAPH).setPlaceholder("Reason").setRequired(true).build()));

        return Modal.create(button + "_submit", getModalLabel(button)).addComponents(container).build();
    }

    private static String getModalLabel(String button) {
        StringBuilder out = new StringBuilder();
        if (button.contains("_kick_")) out.append("Kick ");
        else if (button.contains("_warn_")) out.append("Warn ");
        else if (button.contains("_whitelist_")) out.append("Whitelist ");
        else if (button.contains("_sus_")) out.append("Sus ");
        else {
            if (button.contains("_perm_")) out.append("Permanently ");
            else if (button.contains("_temp_")) out.append("Temporarily ");

            if (button.contains("_ban_")) out.append("Ban ");
            else if (button.contains("_mute_")) out.append("Mute ");
        }

        out.append("a Player");
        return out.toString();
    }
}
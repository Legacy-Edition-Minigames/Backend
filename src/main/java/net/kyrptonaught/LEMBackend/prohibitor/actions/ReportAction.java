package net.kyrptonaught.LEMBackend.prohibitor.actions;

import net.kyrptonaught.LEMBackend.LEMBackend;
import net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule;
import net.kyrptonaught.LEMBackend.prohibitor.entries.PlayerEntry;
import net.kyrptonaught.LEMBackend.prohibitor.entries.ReportEntry;
import net.minecraft.text.Text;

import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.loadUUID;
import static net.kyrptonaught.LEMBackend.prohibitor.ProhibitorModule.saveEntry;

public class ReportAction {
    public static void report(String uuid, String who, String source, String reason, String... evidence) {
        String name = ProhibitorModule.getNameFromUUID(uuid);
        String txt = "Reporting: " + name + " (" + uuid + ")\n" +
                "By: " + who + "\n" +
                "Where: " + source + "\n" +
                "Reason: " + reason;
        String link = LEMBackend.BridgeModule.module.createPlayerReport(name, txt, 365155938497200138L);

        PlayerEntry entry = loadUUID(uuid);
        entry.reports.addFirst(new ReportEntry(link, who, source, reason).addEvidence(evidence));
        saveEntry(entry);
        ProhibitorModule.notifyServer(source, uuid, "report", Text.translatable("gui.socialInteractions.narration.report", Text.literal(entry.associatedName), reason).append("\nBy: ").append(who));
    }
}

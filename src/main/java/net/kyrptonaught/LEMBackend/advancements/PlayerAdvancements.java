package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerAdvancements {
    public final ConcurrentHashMap<String, Advancement> advancements = new ConcurrentHashMap<>();

    public int DataVersion;

    public void setDataVersion(int version) {
        this.DataVersion = version;
    }

    public void removeAdvancement(String advancement, String criteria) {
        if (advancements.containsKey(advancement)) {
            advancements.get(advancement).criteria.remove(criteria);
            advancements.get(advancement).done = false;
            if (advancements.get(advancement).criteria.size() == 0)
                advancements.remove(advancement);
        }
    }

    public void addAdvancementCriteria(String advancementKey, JsonObject advancementJson, boolean forceDone) {
        if (advancements.get(advancementKey) == null)
            advancements.put(advancementKey, new Advancement());
        Advancement advancement = advancements.get(advancementKey);
        for (Map.Entry<String, JsonElement> entry : advancementJson.getAsJsonObject("criteria").entrySet()) {
            advancement.addCriteria(entry.getKey(), entry.getValue().getAsString());
        }
        if (forceDone || (advancementJson.has("done") && advancementJson.get("done").getAsBoolean()))
            advancement.done = advancementJson.get("done").getAsBoolean();
    }

    public static class Advancement {
        final ConcurrentHashMap<String, String> criteria = new ConcurrentHashMap<>();
        boolean done;

        public void addCriteria(String criteria, String unlocked) {
            this.criteria.put(criteria, unlocked);
        }
    }
}

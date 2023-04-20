package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancementHolder {
    private final ConcurrentHashMap<String, Advancement> advancements = new ConcurrentHashMap<>();

    private int DataVersion;

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

    public static class Serializer implements JsonSerializer<AdvancementHolder> {
        @Override
        public JsonElement serialize(AdvancementHolder src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();

            for (String advancementKey : src.advancements.keySet()) {
                Advancement advancement = src.advancements.get(advancementKey);
                JsonObject advancementJson = new JsonObject();
                JsonObject criteriaJson = new JsonObject();
                if (advancement != null) {
                    for (String criteria : advancement.criteria.keySet()) {
                        if (advancement.criteria.get(criteria) != null)
                            criteriaJson.addProperty(criteria, advancement.criteria.get(criteria));
                    }

                    advancementJson.add("criteria", criteriaJson);
                    advancementJson.addProperty("done", advancement.done);
                    object.add(advancementKey, advancementJson);
                }
            }

            object.addProperty("DataVersion", src.DataVersion);
            return object;
        }

        public static AdvancementHolder deserialize(JsonObject object) {
            AdvancementHolder advancementHolder = new AdvancementHolder();
            advancementHolder.setDataVersion(object.remove("DataVersion").getAsInt());
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                advancementHolder.addAdvancementCriteria(entry.getKey(), entry.getValue().getAsJsonObject(), true);
            }

            return advancementHolder;
        }
    }
}

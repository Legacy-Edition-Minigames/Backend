package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

public class AdvancementSerializer implements JsonSerializer<PlayerAdvancements> {

    @Override
    public JsonElement serialize(PlayerAdvancements src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        for (String advancementKey : src.advancements.keySet()) {
            PlayerAdvancements.Advancement advancement = src.advancements.get(advancementKey);
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

    public static PlayerAdvancements deserialize(JsonObject object) {
        PlayerAdvancements playerAdvancements = new PlayerAdvancements();
        playerAdvancements.setDataVersion(object.remove("DataVersion").getAsInt());
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            playerAdvancements.addAdvancementCriteria(entry.getKey(), entry.getValue().getAsJsonObject(), true);
        }

        return playerAdvancements;
    }
}
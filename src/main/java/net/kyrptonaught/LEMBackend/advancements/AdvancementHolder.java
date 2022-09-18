package net.kyrptonaught.LEMBackend.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;

public class AdvancementHolder {
    HashMap<String, JsonElement> advancements = new HashMap<>();

    int DataVersion;

    public void setDataVersion(int version) {
        this.DataVersion = version;
    }

    public void addAdvancement(String name, JsonElement obj) {
        advancements.put(name, obj);
    }

    public void removeAdvancement(String name) {
        advancements.remove(name);
    }

    public void mergeAdvancements(AdvancementHolder other) {
        other.advancements.forEach(this::addAdvancement);
    }

    public static class Serializer implements JsonSerializer<AdvancementHolder> {
        @Override
        public JsonElement serialize(AdvancementHolder src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            src.advancements.forEach(object::add);
            object.addProperty("DataVersion", src.DataVersion);
            return object;
        }

        public static AdvancementHolder deserialize(JsonObject object) {
            AdvancementHolder advancementHolder = new AdvancementHolder();
            advancementHolder.setDataVersion(object.remove("DataVersion").getAsInt());
            object.entrySet().forEach(entry -> advancementHolder.addAdvancement(entry.getKey(), entry.getValue()));
            return advancementHolder;
        }
    }
}

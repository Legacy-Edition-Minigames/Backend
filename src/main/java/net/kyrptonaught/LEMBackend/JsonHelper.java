package net.kyrptonaught.LEMBackend;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Collection;

public class JsonHelper {

    public static JsonArray toJsonArray(Collection<JsonElement> list) {
        JsonArray arr = new JsonArray(list.size());
        for (JsonElement element : list) arr.add(element);
        return arr;
    }
}
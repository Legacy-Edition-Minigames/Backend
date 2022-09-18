package net.kyrptonaught.LEMBackend;

import com.google.gson.Gson;
import io.javalin.plugin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;


public class GsonMapper implements JsonMapper {
    private final Gson gson;

    public GsonMapper(Gson gson) {
        this.gson = gson;
    }

    @NotNull
    @Override
    public String toJsonString(@NotNull Object obj) {
        return gson.toJson(obj);
    }

    @NotNull
    @Override
    public <T> T fromJsonString(@NotNull String json, @NotNull Class<T> targetClass) {
        return gson.fromJson(json, targetClass);
    }
}

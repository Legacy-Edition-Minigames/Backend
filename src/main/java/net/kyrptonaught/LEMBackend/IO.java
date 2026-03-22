package net.kyrptonaught.LEMBackend;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.kyrptonaught.LEMBackend.config.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IO {
    private static ExecutorService executorService;
    public static HttpClient client;

    public static void onInitialize() {
        executorService = Executors.newFixedThreadPool(2);
        client = HttpClient.newBuilder()
                .executor(executorService)
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static void stop() {
        client.shutdown();
        executorService.shutdown();
    }

    public static String getValue(String url, String key) {
        String response = getAlt(url);

        if (response != null && !response.isEmpty()) {
            JsonObject obj = ConfigManager.getGSON().fromJson(response, JsonObject.class);
            if (obj != null && obj.has(key)) {
                if (obj.get(key) instanceof JsonNull) return null;
                return obj.get(key).getAsString();
            }
        }
        return null;
    }

    public static String getAlt(String url) {
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
            return null;
        }

        return didRequestPass(response) ? response.body() : null;
    }

    public static void asyncPostAlt(String url, String json) {
        HttpRequest request = buildPostRequest(url, json);
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private static boolean didRequestPass(HttpResponse<String> response) {
        return response != null && response.statusCode() == 200 && !response.body().equalsIgnoreCase("failed");
    }

    private static HttpRequest buildGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .GET()
                .build();
    }

    private static HttpRequest buildPostRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
    }

    private static HttpRequest buildPostRequest(String url, String json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    public static String getApiUrl(String module) {
        return getApiURL() + "/" + module;
    }

    private static String getApiURL() {
        return "http://localhost:" + LEMBackend.getConfig().port + "/v1/" + LEMBackend.getConfig().secretKey;
    }
}

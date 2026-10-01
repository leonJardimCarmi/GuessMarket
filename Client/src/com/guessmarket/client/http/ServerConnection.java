package com.guessmarket.client.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The only class that talks to the network. Every request goes through execute(), which:
 * sends the session cookie, closes the response (so no connection can leak),
 * turns a JSON answer into the expected DTO, and turns an error answer into a ServerException.
 */
public class ServerConnection {
    // The grader runs the server on this machine; the context path is the WAR's name.
    public static final String BASE_URL = "http://localhost:8080/GuessMarket";

    private final SessionCookieJar cookieJar = new SessionCookieJar();
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public ServerConnection() {
        // DEVELOPMENT ONLY (lecturer's tip): if a response is ever left unclosed, OkHttp also logs where it was
        // created. Remove before submission.
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
        // One client for the whole application: it reuses its open connections between requests.
        client = new OkHttpClient.Builder().cookieJar(cookieJar).build();
    }

    public <T> T get(String path, Map<String, String> query, Type resultType) {
        HttpUrl.Builder url = HttpUrl.get(BASE_URL + path).newBuilder();
        query.forEach(url::addQueryParameter); // encodes spaces and special characters
        return execute(new Request.Builder().url(url.build()).get().build(), resultType);
    }

    public <T> T post(String path, Map<String, String> form, Type resultType) {
        FormBody.Builder body = new FormBody.Builder();
        form.forEach(body::add);
        return execute(new Request.Builder().url(BASE_URL + path).post(body.build()).build(), resultType);
    }

    // Forgets the session cookie (after logging out).
    public void clearSession() {
        cookieJar.clear();
    }

    private <T> T execute(Request request, Type resultType) {
        // try-with-resources: the response is closed whether or not its body is read, and also on exceptions.
        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new ServerException(response.code(), errorMessageOf(response.code(), body));
            }
            return gson.fromJson(body, resultType);
        } catch (IOException e) {
            throw new ServerException(ServerException.NO_CONNECTION,
                    "Cannot reach the server at " + BASE_URL + ". Make sure the server is running.");
        } catch (JsonParseException e) {
            throw new ServerException(ServerException.CLIENT_FAILURE, "The server's answer could not be read.");
        }
    }

    // Our server always answers errors as {"error": "..."}; anything else (e.g. Tomcat's HTML 404 page) gets a general message.
    private String errorMessageOf(int status, String body) {
        try {
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json != null && json.has("error")) {
                return json.get("error").getAsString();
            }
        } catch (JsonParseException ignored) {
            // not JSON - fall through to the general message
        }
        return "The server answered with an error (HTTP " + status + ").";
    }

    /**
     * Keeps the cookies the server sets (the JSESSIONID that identifies our session) and sends them back.
     * Synchronized: requests run on several background threads at once.
     */
    private static final class SessionCookieJar implements CookieJar {
        private final Map<String, List<Cookie>> cookiesByHost = new HashMap<>();

        @Override
        public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            List<Cookie> stored = new ArrayList<>(cookiesByHost.getOrDefault(url.host(), List.of()));
            for (Cookie cookie : cookies) {
                stored.removeIf(old -> old.name().equals(cookie.name())); // a new value replaces the old one
                stored.add(cookie);
            }
            cookiesByHost.put(url.host(), stored);
        }

        @Override
        public synchronized List<Cookie> loadForRequest(HttpUrl url) {
            return cookiesByHost.getOrDefault(url.host(), List.of()).stream()
                    .filter(cookie -> cookie.matches(url))
                    .toList();
        }

        synchronized void clear() {
            cookiesByHost.clear();
        }
    }
}

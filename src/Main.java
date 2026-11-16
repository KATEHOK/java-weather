import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;


public class Main {
    static void main(String[] args) {
        WeatherService weatherService = new WeatherService();
        weatherService.run();
    }
}

class WeatherService {

    private static final String API_URL = "https://api.weather.yandex.ru/v2/forecast";
    private static final String KEY_HEADER = "X-Yandex-Weather-Key";
    private static final String KEY_ENV = "YANDEX_WEATHER_KEY";
    private static final double LAT = 55.75;
    private static final double LON = 37.62;
    private static final int LIMIT = 7;
    private static final int TIMEOUT_SEC = 10;

    private final HttpClient client = HttpClient.newHttpClient();

    private int status;
    private String body;
    private JSONObject json;

    private void println(String txt) {
        System.out.println(txt);
    }

    private String buildUrl() {
        return API_URL + "?lat=" + LAT + "&lon=" + LON + "&limit=" + LIMIT;
    }

    private boolean tryFetch(String key) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(buildUrl()))
                .header(KEY_HEADER, key)
                .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            println("Request failed: " + e);
            return false;
        }
        status = response.statusCode();
        body = response.body();
        return true;
    }

    private boolean tryFetch() {
        String key = System.getenv(KEY_ENV);
        if (key == null || key.isBlank()) {
            println("No API key. Set " + KEY_ENV + " environment variable");
            return false;
        }
        return tryFetch(key.strip());
    }

    private String explainStatus() {
        switch (status) {
            case 400:
                return "Bad request: check query parameters";
            case 403:
                return "Forbidden: key is missing, not active yet, expired or out of requests";
            case 404:
                return "Not found: check lat and lon";
            case 429:
                return "Too many requests: try again later";
            default:
                if (status >= 500) return "Weather service error: try again later";
                return "Unexpected response";
        }
    }

    private boolean tryParse(String src) {
        try {
            json = new JSONObject(src);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    private Number tryNumber(String ptr) {
        if (json.optQuery(ptr) instanceof Number num) return num;
        return null;
    }

    private String format(double num) {
        return String.format(Locale.ROOT, "%.1f", num);
    }

    public void run() {
        if (!tryFetch()) return;

        if (status != 200) {
            println("HTTP " + status + ". " + explainStatus());
            println(body);
            return;
        }

        println(body);
        println("");

        if (!tryParse(body)) {
            println("Response is not a valid JSON");
            return;
        }

        Number temp = tryNumber("/fact/temp");
        println("Current temperature: " + (temp == null ? "unknown" : temp + " C"));

        double sum = 0;
        int count = 0;
        for (int i = 0; i < LIMIT; ++i) {
            Number num = tryNumber("/forecasts/" + i + "/parts/day/temp_avg");
            if (num == null) continue;
            sum += num.doubleValue();
            ++count;
        }
        if (count == 0) {
            println("Average temperature: unknown");
            return;
        }
        println("Average day temperature for " + count + " days: " + format(sum / count) + " C");
    }
}

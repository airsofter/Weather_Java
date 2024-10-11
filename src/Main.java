import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Main {
    private static final String accessKey = "b250d7bc-cd76-41fd-988b-17a2a91706c9";
    private static final String accessHeader = "X-Yandex-Weather-Key";
    private static final String url = "https://api.weather.yandex.ru/v2/forecast";
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        System.out.println("Если не хотите указывать конкретные координаты - можете пропустить нажатием Enter");

        String lat = input("Введите широту:");
        String lon = lat.isEmpty() ? "" : input("Введите долготу:");
        String limit = input("Введите количество дней, для которых посчитать среднюю температуру (не боллее 7):");

        String url = createUrl(lon, lat, limit);
        String responseStringBody = sendHttpRequest(url);

        if (responseStringBody != null) {
            System.out.println("Response Body: " + responseStringBody);
            getResponse(responseStringBody, limit);
        }
    }

    private static String input(String text){
        System.out.println(text);
        return scanner.nextLine().trim();
    }

    private static String createUrl(String lon, String lat, String limit) {
        StringBuilder newUrl = new StringBuilder(url);

        if (!lon.isEmpty()) {
            newUrl.append("?lon=").append(lon).append("&lat=");
        }
        if (!limit.isEmpty() && !lat.isEmpty()) {
            newUrl.append("&limit=").append(limit);
        }
        if (lon.isEmpty() && !limit.isEmpty()) {
            newUrl.append("?limit=").append(limit);
        }
        return newUrl.toString();
    }

    private static String sendHttpRequest(String url) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .headers(accessHeader, accessKey)
                    .GET()
                    .build();


            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Code: " + response.statusCode());
            return response.body();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error making HTTP request: " + e.getMessage());
            return null;
        }
    }

    private static void getResponse(String responseBody, String limit) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse = mapper.readTree(responseBody);
        int temperature = jsonResponse.get("fact").get("temp").asInt();

        System.out.println("Температура сейчас: " + temperature);

        JsonNode forecasts = jsonResponse.get("forecasts");
        if (!limit.isEmpty()) {
            getWeekTemp(forecasts, limit);
        }
    }

    private static void getWeekTemp(JsonNode forecasts, String limit) {
        int forecastSize = forecasts.size();
        double weekTemp = 0;
        double nightWeekTemp = 0;
        double dayWeekTemp = 0;

        for (int i = 0; i < forecastSize; i++) {
            int dayTemp = forecasts.get(i).get("parts").get("day_short").get("temp").asInt();
            int nightTemp = forecasts.get(i).get("parts").get("night_short").get("temp").asInt();

            weekTemp += dayTemp + nightTemp;
            dayWeekTemp += dayTemp;
            nightWeekTemp += nightTemp;
        }
        System.out.println("Средняя температура на " + forecastSize + "д.: " + weekTemp / (forecastSize * 2));
        System.out.println("Средняя температура днем на " + forecastSize + "д.: " + dayWeekTemp / forecastSize);
        System.out.println("Средняя температура ночью на " + forecastSize + "д.: " + nightWeekTemp / forecastSize);
    }
}

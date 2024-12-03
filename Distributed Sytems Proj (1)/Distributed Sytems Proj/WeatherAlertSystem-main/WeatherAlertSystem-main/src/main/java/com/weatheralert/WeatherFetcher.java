package com.weatheralert;

import org.json.JSONException;
import redis.clients.jedis.Jedis;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.JOptionPane;
import java.io.FileReader;

public class WeatherFetcher {
    private static final String REDIS_CHANNEL_ALERTS = "weather_alerts";
    private static final String REDIS_CHANNEL_UPDATES = "weather_updates";
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current_weather=true";
    private boolean useMockData = false; // Set to true for testing with mock data
    private Jedis publishJedis;
    private Jedis preferencesJedis;

    // Constructor to initialize both publish and preferences Jedis clients
    public WeatherFetcher(Jedis publishJedis, Jedis preferencesJedis) {
        this.publishJedis = publishJedis;
        this.preferencesJedis = preferencesJedis;
    }

    public void startFetching() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timer triggered");
                fetchAndPublishWeatherData();
            }
        },  0, 60 * 60 * 1000);

        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                publishDailyWeatherUpdate();
            }
        },  0, 60 * 60 * 1000);
    }

    public void fetchAndPublishWeatherData() {
        try {
            System.out.println("Fetching weather data...");
            String weatherData = fetchWeatherData();
            boolean extremeTemperature = checkForExtremeTemperature(weatherData);
    
            if (extremeTemperature) {
                String message = "Extreme temperature alert! Take precautions.";
                publishJedis.publish(REDIS_CHANNEL_ALERTS, message);
                System.out.println("Published alert: " + message);
                // Publish a message to show the evacuation dialog on the frontend
                publishJedis.publish(REDIS_CHANNEL_ALERTS, "Evacuate");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void publishDailyWeatherUpdate() {
        try {
            String weatherData = fetchWeatherData();
            String updateMessage = getWeatherUpdateMessage(weatherData);
            publishJedis.publish(REDIS_CHANNEL_UPDATES, updateMessage);
            System.out.println("Published daily update: " + updateMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String fetchWeatherData() {
        if (useMockData) {
            try {
                return loadMockData("mock_weather_data.json");
            } catch (Exception e) {
                System.out.println("Error loading mock data: " + e.getMessage());
            }
        }

        // Fetch data from the actual API
        return fetchWeatherFromAPI();
    }

    private String loadMockData(String filePath) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        return stringBuilder.toString();
    }

    private String fetchWeatherFromAPI() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (Exception e) {
            System.out.println("Error fetching weather data from API: " + e.getMessage());
            return "{}"; // Return empty JSON in case of error
        }
    }

    public boolean checkForExtremeTemperature(String weatherData) {
        try {
            JSONObject json = new JSONObject(weatherData);
            if (json.has("current_weather")) {
                JSONObject currentWeather = json.getJSONObject("current_weather");
                double temperature = currentWeather.optDouble("temperature", Double.NaN);

                // Retrieve user preferences (from Redis)
                JSONObject userPreferences = getUserPreferences();
                double preferredMaxTemp = userPreferences.optDouble("temperature_max", Double.NaN);
                double preferredMinTemp = userPreferences.optDouble("temperature_min", Double.NaN);

                // Check if the temperature is extreme based on user preferences
                if (temperature > preferredMaxTemp || temperature < preferredMinTemp) {
                    System.out.println("Extreme temperature detected: " + temperature + "°C");
                    return true;
                } else {
                    System.out.println("Temperature within safe range: " + temperature + "°C");
                }
            } else {
                System.out.println("No 'current_weather' data found in response.");
            }
        } catch (Exception e) {
            System.out.println("Error parsing weather data for extreme temperature check: " + e.getMessage());
        }
        return false;
    }

    private JSONObject getUserPreferences() {
        JSONObject preferences = new JSONObject();
        try {
            // Retrieve temperature preferences from Redis
            String maxTemp = preferencesJedis.hget("user_preferences", "temperature_max");
            String minTemp = preferencesJedis.hget("user_preferences", "temperature_min");

            // Convert them to double values and set defaults if missing
            preferences.put("temperature_max", maxTemp != null ? Double.parseDouble(maxTemp) : 30); // Default to 30°C
            preferences.put("temperature_min", minTemp != null ? Double.parseDouble(minTemp) : 10);  // Default to 8°C
        } catch (Exception e) {
            e.printStackTrace();
        }
        return preferences;
    }

    private String getWeatherUpdateMessage(String weatherData) {
        try {
            JSONObject json = new JSONObject(weatherData);
            if (json.has("current_weather")) {
                JSONObject currentWeather = json.getJSONObject("current_weather");
                double temperature = currentWeather.optDouble("temperature", Double.NaN);
                String comment;

                // Determine message based on temperature
                if (temperature > 30) {
                    comment = "You can wear a T-shirt today!";
                } else if (temperature < 5) {
                    comment = "Don't forget your jacket!";
                } else {
                    comment = "Weather is moderate. Dress comfortably!";
                }

                return "Current temperature: " + temperature + "°C. " + comment;
            } else {
                return "Unable to fetch weather update due to missing 'current_weather' data.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to fetch weather update at this time.";
        }
    }

    public static void main(String[] args) {
        Jedis jedisPublish = new Jedis("localhost", 6379);
        Jedis jedisPreferences = new Jedis("localhost", 6379); // Another Jedis client for preferences
        WeatherFetcher fetcher = new WeatherFetcher(jedisPublish, jedisPreferences);
        fetcher.startFetching();
    }
}

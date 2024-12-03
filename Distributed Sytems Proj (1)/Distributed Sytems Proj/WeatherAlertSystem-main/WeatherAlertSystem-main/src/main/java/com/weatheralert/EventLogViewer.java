package com.weatheralert;

import redis.clients.jedis.Jedis;

import java.util.Set;

public class EventLogViewer {

    private static final String EVENT_LOG_KEY = "weather_event_logs";

    public static void main(String[] args) {
        System.out.println("Displaying logged events...");

        try (Jedis jedis = new Jedis("localhost", 6379)) {
            Set<String> loggedEventIds = jedis.hkeys(EVENT_LOG_KEY);

            if (loggedEventIds.isEmpty()) {
                System.out.println("No events found in the event log.");
                return;
            }

            System.out.println("Replaying Logged Events:");
            for (String eventId : loggedEventIds) {
                String eventData = jedis.hget(EVENT_LOG_KEY, eventId);
                System.out.println("Event ID: " + eventId + ", Data: " + eventData);
            }
        } catch (Exception e) {
            System.out.println("Error fetching logged events: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

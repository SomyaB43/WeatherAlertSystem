package com.weatheralert;

import redis.clients.jedis.Jedis;

public class WeatherAlertSystem {

    public static void main(String[] args) {
        // Get Redis connection
        Jedis jedisPublish = RedisConnectionManager.getJedisConnection();  // Jedis client for publishing
        Jedis jedisPreferences = RedisConnectionManager.getJedisConnection();


        // Start the WeatherFetcher to fetch weather data and publish alerts
        WeatherFetcher fetcher = new WeatherFetcher(jedisPublish, jedisPreferences);
        fetcher.startFetching();

        // Start the WeatherSubscriber to listen for alerts and send notifications
        WeatherSusbcriber notifier = new WeatherSusbcriber();
        notifier.startListening();
    }
}



package com.weatheralert;
import redis.clients.jedis.Jedis;

public class RedisConnectionManager {
    private static Jedis jedis;

    public static Jedis getJedisConnection() {
        if (jedis == null) {
            jedis = new Jedis("localhost", 6379); // Connect to Redis server
        }
        return jedis;
    }

    public static void closeAllConnections() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'closeAllConnections'");
    }
}


const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const redis = require('redis');
const path = require('path');
const bodyParser = require('body-parser'); // For parsing JSON in requests

// Create an Express application
const app = express();
const server = http.createServer(app);
const io = socketIo(server);

const cors = require('cors');
app.use(cors());

// Redis client for regular commands (saving preferences and event logs)
const redisClientForCommands = redis.createClient({
    url: 'redis://127.0.0.1:6379'
});

// Redis client for Pub/Sub (subscribing to channels)
const redisClientForPubSub = redis.createClient({
    url: 'redis://127.0.0.1:6379'
});

// Middleware to parse JSON bodies
app.use(bodyParser.json());

// Event log Redis key
const EVENT_LOG_KEY = 'weather_event_logs';

// Function to log events to Redis
async function logEvent(type, channel, message) {
    try {
        const eventId = `${Date.now()}`; // Use timestamp as unique event ID
        const eventData = JSON.stringify({ type, channel, message, timestamp: new Date().toISOString() });
        await redisClientForCommands.hSet(EVENT_LOG_KEY, eventId, eventData);
        console.log(`Event logged: ${eventData}`);
    } catch (error) {
        console.error('Error logging event:', error);
    }
}

async function startRedis() {
    try {
        // Connect both Redis clients
        await redisClientForCommands.connect();
        await redisClientForPubSub.connect();

        console.log('Redis clients connected');

        // Subscribe to Redis channels using the pub/sub client
        await redisClientForPubSub.subscribe('weather_alerts', async (message) => {
            io.emit('weather_alerts', message);
            console.log(`Message received on weather_alerts: ${message}`);
            await logEvent('alert', 'weather_alerts', message);
        });

        await redisClientForPubSub.subscribe('weather_updates', async (message) => {
            io.emit('weather_updates', message);
            console.log(`Message received on weather_updates: ${message}`);
            await logEvent('update', 'weather_updates', message);
        });

        redisClientForPubSub.subscribe('weather_alerts', async (message) => {
            console.log("Message received on weather_alerts:", message);

            if (message === 'evacuate') {
                io.emit('evacuation_alert', 'evacuate');
                console.log("Evacuation alert emitted to frontend:", message);
                await logEvent('alert', 'weather_alerts', message);
            } else {
                io.emit('weather_alerts', message);
                await logEvent('alert', 'weather_alerts', message);
            }
        });

        console.log('Subscribed to channels: weather_alerts, weather_updates');
    } catch (error) {
        console.error('Redis connection error:', error);
    }
}

startRedis();

// Serve the index.html file when accessing the root URL
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

// Handle saving user preferences (POST request)
app.post('/api/preferences', async (req, res) => {
    const { temperature, windSpeed, humidity } = req.body;

    // Store the preferences using the regular Redis client
    try {
        await redisClientForCommands.hSet('user_preferences', 'temperature', temperature);
        await redisClientForCommands.hSet('user_preferences', 'windSpeed', windSpeed);
        await redisClientForCommands.hSet('user_preferences', 'humidity', humidity);

        res.json({ message: 'Preferences saved successfully!' });
    } catch (error) {
        console.error('Error saving preferences:', error);
        res.status(500).json({ message: 'Failed to save preferences' });
    }
});

// New API endpoint to fetch logged events
app.get('/api/event-logs', async (req, res) => {
    try {
        const keys = await redisClientForCommands.hKeys(EVENT_LOG_KEY);

        if (keys.length === 0) {
            return res.json({ message: 'No events found in the event log.' });
        }

        const logs = {};
        for (const key of keys) {
            logs[key] = await redisClientForCommands.hGet(EVENT_LOG_KEY, key);
        }

        res.json({ logs });
    } catch (error) {
        console.error('Error fetching event logs:', error);
        res.status(500).json({ error: 'Failed to fetch event logs.' });
    }
});

// Socket.IO connection handler
io.on('connection', (socket) => {
    console.log('A user connected');

    // Handle client disconnection
    socket.on('disconnect', () => {
        console.log('A user disconnected');
    });
});

// Start the server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

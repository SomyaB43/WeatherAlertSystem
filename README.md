1. Install WSL, Using this command in terminal wsl --install
   This will install and run WSL for you

TEST PUBLISH-SUBSCRIBE MODEL 

- To test publish and subscribe functionality you can do the following steps:
- enter redis-cli in the terminal running WSL, then enter this command to subscribe: SUBSCRIBE weather_alerts
- next open another terminal and enter wsl to start wsl, then enter this command to publish: PUBLISH weather_alerts "Please head to nearest emergency shelter now"
- when done close terminals

RUN APPLICATION:
1. start WSL in new terminal by using command wsl
2. Then run our run index.js located in the weather-alert-server folder
3. Then view application by going to http://localhost:3000/
5. next run weatherfetcher.java, and the application will send a alert evacuation message, click okay and you will be able to view the weather alert, and the opion to view emergency shelter locations
6. By running weatherfetcher.java it will also tell you the current weather under weather updates
7. You can use the temperature, wind speed, and humidity controls to set your custom alerts 
8. Event log: View the event log by clicking the "show logged events button" on the application


  

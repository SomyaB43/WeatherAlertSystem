1. Install WSL, Using this command in terminal wsl --install
   This will install and run WSL for you

TEST PUBLISH-SUBSCRIBE MODEL 

- To test publish and subscribe functionality you can do the following steps:
- enter redis-cli in the terminal running WSL, then enter this command to subscribe: SUBSCRIBE weather_alerts
- next open another terminal and enter wsl to start wsl, then enter this command to publish: PUBLISH weather_alerts "Please head to nearest emergency shelter now"
- when done close terminals

RUN APPLICATION:
1. start WSL in new terminal by using command wsl
2. then use this command to start redis server you will also be prompted to enter sudo password: sudo service redis-server start
4. Then run our run index.js located in the weather-alert-server folder
5. Then view application by going to http://localhost:3000/
6. next run weatherfetcher.java, and the application will send a alert evacuation message, click okay and you will be able to view the weather alert, and the opion to view emergency shelter locations
7. By running weatherfetcher.java it will also tell you the current weather under weather updates
8. You can use the temperature, wind speed, and humidity controls to set your custom alerts 
9. Event log: View the event log by clicking the "show logged events button" on the application


  

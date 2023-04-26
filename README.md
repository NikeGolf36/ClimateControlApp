# Climate Control Application
This is our Android application for Group 20 Senior Design Project: Sensor System for In-Home Climate Control.
## How it works
Our application give users the ability to add sensor nodes to their setup. BLE WiFi provisoning using the ESP32 expressif provisioning libraries allows sensors to connect to the local network. Sensor data is updated from the InfluxDB cloud database using a Worker class running in the background. The Worker class also computes the window state based on the newly updated data and executes the process for push notifications. Users have the ability to define sensors and indoor/outdoor and enter their desired temperature. Additional sensor information can be found when tapping on a sensor name and graphs are built using the last 24 hours of data.  
## Setup
1. Open repo in Android Studio
2. Create InfluxDB Cloud 2.0 account
3. Copy url, token, and org numbers and define in a file named secrets.xml in the app/res/values folder (make sure these values are the same within the secrets folder on the ESP32)
4. Connect android device and download application

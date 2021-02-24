# circles
![](https://github.com/timothydillan/300cem-android-app/blob/main/app/src/main/res/drawable-mdpi/logo.png)

### Background
Due to hurried creation of the video, a technical explanation will be elaborated below to highlight some of the features that were not mentioned in the video.

### Features
- Allows users to share their location with their closest ones (circle).
- Allow users to view each other's health information.
- Allow users to view each other's mood information.
- Able to detect if a user has encountered a crash.
- Users are able to alert other members using the SOS alert feature.
- Users are able to edit their circle name.
- Users are able to create a new circle.
- Users are able to join an existing circle.
- Users are able to leave the circle that they are currently in.
- Users are able to edit their profile details.
- Users are able to remove their account.
- Users are able to turn on password and biometric authentication, so that other people using the user's device won't be able to view sensitive information.
- Users are able to pair their device with their wearables (if the wearable is using WearOS). So that the wearable is able to send data to their handheld device, users would need to first download this project, and change the configuration module to the circles_wear module, and then run the app. A video will be shown below.

# Sensors Used

##### Heart Rate (Wearable)
##### Pedometer (Wearable)
##### Step Detector
##### Pedometer
##### Significant Motion
##### GPS Sensor
##### Biometric Sensor (may include Face ID, Iris, or Fingerprint)

# APIs Used
To begin, the APIs used are listed as follows:

##### AndroidX Biometric
##### Material Design
##### Firebase
- Authentication
- Database
- Storage
- Messaging
##### Google Play Services
- Maps
- Location
- Activity Recognition
- Wearable

*The auth api won't be elaborated here, so a short explanation for it is that it is used to ease the authentication process by allowing users to create an account, or sign in with an account, and all the "back-end" stuff is mainly handled by Firebase.*

# Videos
### Phone
https://streamable.com/ofefeq

https://streamable.com/oi1szt

https://streamable.com/8ni5mk

### Wearable
https://streamable.com/bxmc91

https://streamable.com/qwk7ay

# Location Sharing
To conduct location sharing, the application uses the Database API to update the user's latitude and longitude in real-time. To be able to accomplish this, a fused location provider with a custom location callback listener was used. Once the location of the user is changed, the callback triggers the `onLocationResult` event, which then updates the user's location in the database. A code-by-code explanation can be seen [here](https://github.com/timothydillan/300cem-android-app/blob/main/app/src/main/java/com/timothydillan/circles/Services/LocationService.java).

When the location of the user is updated, the onUsersChange event will be triggered for every listener that is registered/listening to the events provided by the UserUtil class. The event, when triggered, provides us with a DataSnapshot (basically new data from the DB), which we will then use to get new information about the user, as seen [here](https://github.com/timothydillan/300cem-android-app/blob/main/app/src/main/java/com/timothydillan/circles/MapsFragment.java#L227).

Now, to get information that the user cares about (only their circle information), the MapsFragment overrides the default onUsersChange behavior and calls the getUpdatedInformation function from the Location utility class to provide the users with new information. All processing of data filtering will not be explained, however, the full explanation for the data filtering can be seen [here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/Utils/LocationUtil.java#L184).

### Extra

A custom bottom sheet layout, combined with a custom recycler view, was used to display detailed information about each user, such as the address of their current location ([reverse geocoding of their latitude and longitude was done to do this](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/Utils/LocationUtil.java#L301)). Also, each user's username, as well as their profile picture is shown in the custom recycler view. A custom recycler view adapter was also used to allow users to immediately redirect ([move their map camera position to a user's marker](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/MapsFragment.java#L137)). Also, when each user's marker is clicked, an info window will be shown, and when a user clicks on a specific marker, they would be able to get directions to the person clicked ([this uses a google map intent that passes in the latitude and longitude of the user](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/MapsFragment.java#L186)).

# Moods
The Moods fragment, similar to the Maps fragment, also uses the Database API, combined with the Wearable API to update and retrieve information. The Moods fragment, when launched, first checks whether user has a wearable paired with their handheld device. The application does this by checking whether the user has the WearOS app (this is a really "ghetto" way to check whether the user has a wearable paired, but due to time constraints this is what I have to settled with). If the user has a wearable paired, the Moods fragment will trigger the WearableService, which inherits the WearableListenerService, that uses the Data Layer API (https://developer.android.com/training/wearables/data-layer/events) to receive data from the wearable, to the app. When data is retrieved from the wearable, the onDataChange event will be triggered in the service, and the data from the wearable (heart rate) will be retrieved, and the Database API will be used to update the user's heart rate data. Once the heart rate data is updated, the MoodUtil class will receive the new information, and assign the user's mood according to their heartbeat. Details about the calculation for each mood is elaborated in the report, and can be seen [here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/Utils/MoodUtil.java#L41).

### Extra

A custom recycler view is used in this fragment to show each user's mood information. A custom recycler adapter was also used in this fragment to allow users to click on each item, which will allow the user to WhatsApp the member clicked, with a placeholder message according to the member's mood, as seen [here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/MoodsFragment.java#L95).

# Health
Similar to the Moods fragment, the Health fragment also uses the Database API and the data from the wearable paired. When data is retrieved from the wearable, the onDataChange event will be triggered in the service, and the data from the wearable (heart rate and step count/pedometer data) will be retrieved, and the Database API will be used to update the user's heart rate and step count data. The Health fragment features two separate "tabs" that allow the users to view each user's health information for today ([see here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/HealthFragment.java#L216)), and the other "tab" showing a summary of the circle's health information today, a week ago, and a month ago ([see here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/HealthFragment.java#L227)).

### Extra

A custom recycler view is used in this fragment to show each user's health information as [seen here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/Adapters/HealthRecyclerAdapter.java#L98), with an additional, maybe unnoticable, heart pulsating animation ([see here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/Adapters/HealthRecyclerAdapter.java#L68)):0. A custom recycler view also used to show a summary of the circle's health information, as [seen here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/Adapters/CircleHealthRecyclerAdapter.java#L72).

# Crash Detection
The crash detection feature uses the accelerometer sensor in the user's device, as well as the Activity Recognition API that uses low-level sensors (step counter, step detector, significant motion) to detect whether the user has encountered a car crash. Basically, when the crash detection feature is enabled, configuration data will be written unto the app's sharedpreferences, and the crash service will be started. When the crash service is initialized, the accelerometer sensor will be registered [see here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/app/src/main/java/com/timothydillan/circles/Services/CrashService.java#L94), and the activity recognition API will be initialized, and a receiver that listens to activity changes from the activity recognitino API will be registered. Once the accelerometer sensor is initialized, a CrashListener instance is created, and the class will use the data from the accelerometer to detect whether the user has encountered a crash (checks whether the force applied to the device goes above the threshold we set). If the CrashListener sees that the force applied to the device goes above the threshold, the `onCrash` event will be called, which will then be handled in the CrashService. When the `onCrash` event is triggered, the CrashService first checks whether the current activity of the user is that they are in a vehicle. If the user is in the vehicle, and the `onCrash` event was triggered, then the user has possibly encountered a crash, and so the Crash Confirmation activity is triggered to verify whether the user really did encounter a crash.

### Extra
The crash confirmation activity uses the Firebase Messaging API to notify the user's circle members that they have encountered a car crash. Details about this can be seen [here](https://github.com/timothydillan/300cem-android-app/blob/main/app/src/main/java/com/timothydillan/circles/Utils/NotificationUtil.java) and [here]().

# Biometric Authentication
The biometric authentication feature allows users to protect the app from being accessible by people that may have took hold of the users device. As the application contains several sensitive information, such as where each user is, their mood information, their phone number, and their health information, it may be crucial to provide authentication for the application.

The Biometric authentication process, with the help of the AndroidX Biometric API, uses the Biometric sensor available on the user's device to authenticate the user. As the AndroidX Biometric API does not provide any feature for manual selection of the Biometric that will be used, every device using the application may have different options for the biometric authentication, such as using their Face ID, or Fingeprint, or their Iris to authenticate the user. 

The authentication process is mainly managed using the security utilitiy class that can be found [here](https://github.com/timothydillan/300cem-android-app/blob/main/app/src/main/java/com/timothydillan/circles/Utils/SecurityUtil.java).

# Wearable Module
The Wearable module of the application can be viewed on the circles_wear module of the project's application structure. The Wearable module for the application uses the DataLayer API to communicate with the handheld device paired. The main use of the wearable module is to send sensor data from the wearable, to the handheld device for further processing.

When the application is first launched on a wearable, the application will request for permissions on reading the user's body sensors. Once granted, the application would start the SensorService, which would initialize the sensors, and send data using a PutDataRequest that can be seen [here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/circles_wear/src/main/java/com/timothydillan/circles/SensorService.java#L123) to the paired device, everytime a new value is received for the step count and the heartbeat of the user. The data will also be sent to a [BroadcastReceiver](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/circles_wear/src/main/java/com/timothydillan/circles/SensorService.java#L135), so that the sensor data can be shown in the user's wearable, where the MainActivity will register a BroadcastReceiver that listens to the data sent from the service, as seen [here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/circles_wear/src/main/java/com/timothydillan/circles/MainActivity.java#L114), and [here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/circles_wear/src/main/java/com/timothydillan/circles/MainActivity.java#L131). The main reason why the sensor sevice is used, is so that the application can read sensor data in the background.

As the wearable module does not know the name of the user, the MainActivity registers a DataClient listener that listens for data items sent from the handheld device. The handheld device will send the user's username, their cycling, running, and walking time information, as well as their previous step count, so that the user will be able to see these data in the wearable as well. The code for this can be seen [here](https://github.com/timothydillan/300cem-android-app/blob/d9889e950b1cc7b4ff0be97defd35ac60d019423/circles_wear/src/main/java/com/timothydillan/circles/MainActivity.java#L143).


### End
Other features, such as the join circle, leave circle, managing circle members, can be viewed immediately on the application's source code.

There are many improvements that can be done to the application's source code, and there are several bugs that will be fixed soon.

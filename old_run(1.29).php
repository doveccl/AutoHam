#!/usr/bin/env php
<?php
	$api = "http://c.aipao.me/api";
	
	function get($_url) {
		$_c = curl_init();
		curl_setopt($_c, CURLOPT_URL, $_url);
		curl_setopt ($_c, CURLOPT_RETURNTRANSFER, true);
		return json_decode(curl_exec($_c));
	}
	function getMap() {
		$map = [];
		for ($i = ord('a'); $i <= ord('z'); $i++)
			$map []= chr($i);
		shuffle($map);
		while (count($map) > 10)
			array_pop($map);
		return $map;
	}	
	function encrypt($val, $map) {
		$val = strval($val);
		for ($i = 0; $i < 10; $i++)
			$val = str_replace("$i", $map[$i], $val);
		return $val;
	}
	function getToken($code, $imei) {
		global $api;
		$q = "$api/$code/QM_Users/Login?wxCode=$code&IMEI=$imei";
		echo "Use the api: $q\n";
		$msg = get($q);
		echo "The raw msg: ";
		var_dump($msg);
		if ($msg->Success) {
			$token = $msg->Data->Token;
			echo "The token is: $token\n";
			return $token;
		}
		else
			die("Failed to get token!\n");
	}
	function getUserInfo($token) {
		global $api;
		$q = "$api/$token/QM_Users/GetLoginInfoByUserId";
		echo "Use the api: $q\n";
		$info = get($q);
		echo "The raw msg: ";
		var_dump($info);
		if ($info->Success)
			return $info;
		else
			die("Failed to get info!\n");
	}
	function setLastLatLngByField($token, $uid, $lat, $lng) {
		global $api;
		$q = "$api/$token/QM_User_Field/SetLastLatLngByField?UserId=$uid&FieldId=0&Lat=$lat&Lng=$lng";
		echo "Use the api: $q\n";
		$msg = get($q);
		echo "The raw msg: ";
		var_dump($msg);
		if ($msg->Success)
			echo "Set success!\n";
		else
			echo "Failed to set!\n";
	}
	function getSignReward($token) {
		global $api;
		$q = "$api/$token/QM_Users/GetSignReward";
		echo "Use the api: $q\n";
		$msg = get($q);
		echo "The raw msg: ";
		var_dump($msg);
		if ($msg->Success)
			echo "Signed success!\n";
		else
			echo "Failed to sign!\n";
	}
	function wait($time) {
		$stime = time();
		while (time() <= $stime + $time) {
			echo "Please wait ";
			$rest = $stime + $time - time();
			$rest = str_pad($rest, 4, '0', STR_PAD_LEFT);
			echo $rest . ' s';
			$len = strlen($rest) + 2;
			sleep(1);
			echo "\r";
		}
		echo "\r";
	}

	echo "Input your code & IMEI :";
	fscanf(STDIN, "%s", $tmp);
	$tmp = split("&", $tmp);
	$code = trim($tmp[0]);
	$imei = trim($tmp[1]);

	//left-down 114.367152,30.533393
	//right-up 114.368055,30.534676
	$lat = rand(30533393, 30534676) / 1000000.0;
	$lng = rand(114367152, 114368055) / 1000000.0;

	$token = getToken($code, $imei);
	$info = getUserInfo($token); //*/
/*
	$token = "test0test0test0test0test";
	$info = json_decode('{"Data":{"User":{"UserID":10000},"SchoolRun":{"Lengths":2000}}}'); //*/

	$uid = $info->Data->User->UserID;
	$len = $info->Data->SchoolRun->Lengths;
	wait(5);
	setLastLatLngByField($token, $uid, $lat, $lng);
	wait(5);
	getSignReward($token);
	wait(2);

	$lat = rand(30533393, 30534676) / 1000000.0;
	$lng = rand(114367152, 114368055) / 1000000.0;
	$map = getMap();
	$runLen = $len + rand(1, 10);
	$runTime = rand(9 * 60, 18 * 60);
	$score = 5000; //$runTime + 3 * $runLen;
	$gold = 2000; //$runLen;
	$_runLen = encrypt($runLen, $map);
	$_runTime = encrypt($runTime, $map);
	$_score = encrypt($score, $map);
	$_gold = encrypt($gold, $map);
	$_map = implode("", $map);

	$start = "$api/$token/QM_Runs/StartRunForSchool?Lat=$lat&Lng=$lng&RunType=1&RunMode=1&FUserId=0&Level_Length=$len&IsSchool=1";
	echo "Start run: $start\n";
	$runInfo = get($start);
	echo "Raw res: ";var_dump($runInfo);
	$rid = $runInfo->Data->RunId;

	wait($runTime);

	$end = "$api/$token/QM_Runs/EndRunForSchool?S1=$rid&S2=$_score&S3=$_gold&S4=$_runTime&S5=$_runLen&S6=&S7=1&S8=$_map";
	echo "End run: $end\n";
	$final = get($end);
	echo "Raw res: ";var_dump($final);

	echo "Finished! Exit...\n";
?>
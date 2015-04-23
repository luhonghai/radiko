<?php
$timezone = "Asia/Tokyo";
$cachedDir = sys_get_temp_dir() . "/radioservercache";
$nhkApiToken = "rP3N5A1nBCixRk21AAyDFrIWKtALEpDw";

date_default_timezone_set($timezone);
if (!isset($_GET["provider"]) || !isset($_GET["area"]) || !isset($_GET["channel"])) {
    echo "Error: Invalid parameter";
    die();
}
if (!file_exists($cachedDir)) {
    mkdir($cachedDir, 0744);
}
$provider = isset($_GET["provider"]) ? $_GET["provider"] : "nhk";
$area = isset($_GET["area"]) ? $_GET["area"] : "130";
$channel = isset($_GET["channel"]) ? $_GET["channel"] : "r1";
$date = isset($_GET["date"]) ? $_GET["date"] : "now";
if ($date == 'now') {
    $mDate = date("Y-m-d");
} else {
    $aDate = date_parse_from_format("Y-m-d", $date);
    if ($aDate["error_count"] > 0) {
        echo "Error: Invalid date format. Should be Y-m-d";
        die();
    }
    $mDate = date($aDate["year"] . "-" . $aDate["month"] . "-" . $aDate["day"]);
    if (strtotime($mDate) > strtotime(date("Y-m-d"))) {
        echo "Error: Invalid date range. Should not be future date.";
        die();
    }
}
$sTimeMs = round(microtime(true) * 1000);
// Do we need to split channel program by Area?
$cachedFile = $cachedDir . "/" . strtolower($provider . "_" . $channel . "_" . $area . "_" . $mDate . "_" . date('H') . ".json");
//$cachedFile = $cachedDir . "/" . strtolower($provider . "_" . $channel . "_" . $mDate . ".json");
if (file_exists($cachedFile)) {
    $fContent = file_get_contents($cachedFile);
    $result = json_decode($fContent, true);
} else {

    $result = array();
    $result["cachedTime"] = $sTimeMs;
    $result["channelId"] = strtolower($provider . "_" . $channel);
    $result["service"] = $provider;
    $result["serviceChannelId"] = $channel;
    $result["date"] = $mDate;
    $result["timezone"] = $timezone;
    $result["programs"] = array();
    if (strtolower($provider) == 'radiko') {
        $rXml = file_get_contents("http://radiko.jp/v2/api/program/today?area_id=" . $area);
        if ($rXml === false || empty($rXml)) {
            echo "Error: Could not fetch data from Radiko server";
            die();
        }
        $array = json_decode(json_encode(simplexml_load_string($rXml)), true);
        $stations = $array["stations"]["station"];
        foreach ($stations as $station) {
            if (strtolower($station["@attributes"]["id"]) == strtolower($channel)) {
                $programs = $station["scd"]["progs"]["prog"];
                foreach ($programs as $program) {
                    $aProgram = array();
                    $aProgram["title"] = is_array($program["title"]) ? "" : $program["title"];
                    $aProgram["subTitle"] = is_array($program["sub_title"]) ? "" : $program["sub_title"];
                    $aProgram["description"] = is_array($program["desc"]) ? "" : $program["desc"];
                    $aProgram["information"] = is_array($program["info"]) ? "" : $program["info"];

                    $mFt = $program["@attributes"]["ft"];
                    $mTo = $program["@attributes"]["to"];
                    $aFt = date_parse_from_format("YmdHis", $mFt);
                    $aTo = date_parse_from_format("YmdHis", $mTo);

                    $aProgram["fromTime"] = mktime($aFt["hour"], $aFt["minute"], $aFt["second"], $aFt["month"], $aFt["day"], $aFt["year"]) * 1000;
                    $aProgram["toTime"] = mktime($aTo["hour"], $aTo["minute"], $aTo["second"], $aTo["month"], $aTo["day"], $aTo["year"]) * 1000;
                    array_push($result["programs"], $aProgram);
                }
                break;
            }
        }
    } else if (strtolower($provider) == 'nhk') {
        $rJson = file_get_contents("http://api.nhk.or.jp/v1/pg/list/" . $area. "/" . $channel . "/" . $mDate . ".json?key=" . $nhkApiToken);
        if ($rJson === false || empty($rJson)) {
            echo "Error: Could not fetch data from NHK server";
            die();
        }
        $mJson = json_decode($rJson, true);
        if (isset($mJson["error"])) {
            echo "Error: " . $mJson["error"]["message"];
            die();
        }
        $programs = $mJson["list"][$channel];
        foreach($programs as $program) {
            $aProgram = array();
            $aProgram["title"] = is_array($program["title"]) ? "" : $program["title"];
            $aProgram["subTitle"] = is_array($program["subtitle"]) ? "" : $program["subtitle"];

            $aProgram["information"] = "";
            $aProgram["description"] = "";

            $mFt = $program["start_time"];
            $mTo = $program["end_time"];

            $aFt = date_parse_from_format(DATE_W3C, $mFt);
            $aTo = date_parse_from_format(DATE_W3C, $mTo);

            $aProgram["fromTime"] = mktime($aFt["hour"], $aFt["minute"], $aFt["second"], $aFt["month"], $aFt["day"], $aFt["year"]) * 1000;
            $aProgram["toTime"] = mktime($aTo["hour"], $aTo["minute"], $aTo["second"], $aTo["month"], $aTo["day"], $aTo["year"]) * 1000;

            array_push($result["programs"], $aProgram);
        }
    }
    file_put_contents($cachedFile, json_encode($result));
}
$result["serverTime"] = $sTimeMs;
echo json_encode($result);


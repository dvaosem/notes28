<?php
/* N28: načíta súkromný iCal (Google Kalendár) pre appku – CORS proxy.
   Pustí LEN adresy https://calendar.google.com/calendar/ical/.../*.ics (žiadny otvorený proxy).
   URL ide v tele POST (nie v query), aby sa tajná adresa nezapisovala do access logov. */
$origin = $_SERVER['HTTP_ORIGIN'] ?? '';
if ($origin === 'https://dvaosem.com' || $origin === 'https://www.dvaosem.com') {
  header('Access-Control-Allow-Origin: ' . $origin);
  header('Vary: Origin');
  header('Access-Control-Allow-Methods: POST, OPTIONS');
  header('Access-Control-Allow-Headers: Content-Type');
}
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(204); exit; }
header('Cache-Control: no-store');
if ($_SERVER['REQUEST_METHOD'] !== 'POST') { http_response_code(405); exit('method'); }

$in = json_decode(file_get_contents('php://input'), true);
$url = is_array($in) ? trim((string)($in['url'] ?? '')) : '';
// webcal:// → https://
$url = preg_replace('~^webcal://~i', 'https://', $url);
if (!preg_match('~^https://calendar\.google\.com/calendar/ical/[A-Za-z0-9%@._\-]+/[A-Za-z0-9%@._\-]+/basic\.ics$~', $url)) {
  http_response_code(400); exit('bad url');
}

$body = false; $code = 0;
if (function_exists('curl_init')) {
  $ch = curl_init($url);
  curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true, CURLOPT_FOLLOWLOCATION => false, CURLOPT_TIMEOUT => 12,
    CURLOPT_CONNECTTIMEOUT => 6, CURLOPT_USERAGENT => 'N28-calendar/1.0', CURLOPT_PROTOCOLS => CURLPROTO_HTTPS,
  ]);
  $body = curl_exec($ch);
  $code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
  curl_close($ch);
} else {
  $ctx = stream_context_create(['http' => ['timeout' => 12, 'follow_location' => 0, 'user_agent' => 'N28-calendar/1.0']]);
  $body = @file_get_contents($url, false, $ctx);
  $code = $body === false ? 502 : 200;
}
if ($body === false || $code !== 200) { http_response_code(502); exit('upstream ' . $code); }
if (strlen($body) > 6 * 1024 * 1024 || strpos($body, 'BEGIN:VCALENDAR') === false) { http_response_code(502); exit('not ical'); }
header('Content-Type: text/calendar; charset=utf-8');
echo $body;

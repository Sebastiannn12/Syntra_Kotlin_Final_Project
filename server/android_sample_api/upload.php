<?php

declare(strict_types=1);

require_once __DIR__ . '/db.php';

$pdo = getConnection();
requireAuthenticatedUser($pdo);

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !isset($_FILES['photo'])) {
    respond(['success' => false, 'message' => 'Choose a photo to upload'], 422);
}

$photo = $_FILES['photo'];
if ($photo['error'] !== UPLOAD_ERR_OK || $photo['size'] > 2 * 1024 * 1024) {
    respond(['success' => false, 'message' => 'Photo must be smaller than 2 MB'], 422);
}

$mime = (new finfo(FILEINFO_MIME_TYPE))->file($photo['tmp_name']);
$extensions = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
if (!isset($extensions[$mime])) {
    respond(['success' => false, 'message' => 'Use a JPG, PNG, or WEBP photo'], 422);
}

$directory = __DIR__ . '/uploads';
if (!is_dir($directory) && !mkdir($directory, 0755, true)) {
    respond(['success' => false, 'message' => 'Upload directory is unavailable'], 500);
}
$filename = bin2hex(random_bytes(16)) . '.' . $extensions[$mime];
if (!move_uploaded_file($photo['tmp_name'], $directory . '/' . $filename)) {
    respond(['success' => false, 'message' => 'Photo upload failed'], 500);
}

$scheme = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$path = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$url = $scheme . '://' . $_SERVER['HTTP_HOST'] . $path . '/uploads/' . $filename;
respond(['success' => true, 'message' => 'Photo uploaded', 'photo' => $url]);

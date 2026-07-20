<?php

declare(strict_types=1);

require_once __DIR__ . '/db.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    respond(['success' => true, 'message' => 'OK']);
}

$pdo = getConnection();
$currentUser = requireAuthenticatedUser($pdo);
$action = $_GET['action'] ?? '';
$id = isset($_GET['id']) ? (int) $_GET['id'] : 0;
$input = jsonInput();

function strongPassword(string $password): bool
{
    return strlen($password) >= 8
        && preg_match('/[A-Z]/', $password)
        && preg_match('/[a-z]/', $password)
        && preg_match('/\d/', $password)
        && preg_match('/[^A-Za-z0-9]/', $password);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $action === 'change') {
    $old = (string) ($input['current_password'] ?? '');
    $new = (string) ($input['new_password'] ?? '');
    if (!password_verify($old, $currentUser['password'])) {
        respond(['success' => false, 'message' => 'Current password is incorrect'], 422);
    }
    if (!strongPassword($new)) {
        respond(['success' => false, 'message' => 'Use 8+ characters with uppercase, lowercase, number, and symbol'], 422);
    }
    $pdo->prepare('UPDATE tblusers SET password = ? WHERE id = ?')
        ->execute([password_hash($new, PASSWORD_DEFAULT), (int) $currentUser['id']]);
    $pdo->prepare('DELETE FROM api_tokens WHERE user_id = ?')->execute([(int) $currentUser['id']]);
    respond(['success' => true, 'message' => 'Password changed. Sign in again.']);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $action === 'reset' && $id > 0) {
    $temporary = 'Temp!' . bin2hex(random_bytes(4)) . 'A1';
    $stmt = $pdo->prepare('UPDATE tblusers SET password = ? WHERE id = ?');
    $stmt->execute([password_hash($temporary, PASSWORD_DEFAULT), $id]);
    if ($stmt->rowCount() === 0) {
        respond(['success' => false, 'message' => 'User not found'], 404);
    }
    $pdo->prepare('DELETE FROM api_tokens WHERE user_id = ?')->execute([$id]);
    respond([
        'success' => true,
        'message' => 'Temporary password created',
        'temporary_password' => $temporary,
    ]);
}

respond(['success' => false, 'message' => 'Unsupported request'], 405);

<?php

declare(strict_types=1);

require_once __DIR__ . '/db.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    respond(['success' => true, 'message' => 'OK']);
}

$pdo = getConnection();
$action = $_GET['action'] ?? '';
$input = jsonInput();

function issueToken(PDO $pdo, int $userId): string
{
    $token = bin2hex(random_bytes(32));
    $hash = hash('sha256', $token);
    $pdo->prepare('DELETE FROM api_tokens WHERE user_id = ? OR expires_at <= NOW()')
        ->execute([$userId]);
    $pdo->prepare(
        'INSERT INTO api_tokens (user_id, token_hash, expires_at)
         VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 30 DAY))'
    )->execute([$userId, $hash]);
    return $token;
}

function validateRegistration(array $input): void
{
    foreach (['username', 'first_name', 'last_name', 'email', 'password'] as $field) {
        if (trim((string) ($input[$field] ?? '')) === '') {
            respond(['success' => false, 'message' => str_replace('_', ' ', ucfirst($field)) . ' is required'], 422);
        }
    }
    if (!filter_var($input['email'], FILTER_VALIDATE_EMAIL)) {
        respond(['success' => false, 'message' => 'Enter a valid email address'], 422);
    }
    $password = (string) $input['password'];
    if (strlen($password) < 8 || !preg_match('/[A-Z]/', $password)
        || !preg_match('/[a-z]/', $password) || !preg_match('/\d/', $password)
        || !preg_match('/[^A-Za-z0-9]/', $password)) {
        respond(['success' => false, 'message' => 'Use 8+ characters with uppercase, lowercase, number, and symbol'], 422);
    }
}

try {
    if ($_SERVER['REQUEST_METHOD'] === 'POST' && $action === 'register') {
        validateRegistration($input);
        $stmt = $pdo->prepare(
            'INSERT INTO tblusers (username, last_name, first_name, middle_name, email, password, photo)
             VALUES (?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            trim((string) $input['username']),
            trim((string) $input['last_name']),
            trim((string) $input['first_name']),
            trim((string) ($input['middle_name'] ?? '')),
            strtolower(trim((string) $input['email'])),
            password_hash((string) $input['password'], PASSWORD_DEFAULT),
            trim((string) ($input['photo'] ?? '')),
        ]);
        $id = (int) $pdo->lastInsertId();
        $user = $pdo->query('SELECT * FROM tblusers WHERE id = ' . $id)->fetch();
        respond([
            'success' => true,
            'message' => 'Account created',
            'token' => issueToken($pdo, $id),
            'user' => publicUser($user),
        ], 201);
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST' && $action === 'login') {
        $identity = trim((string) ($input['identity'] ?? ''));
        $password = (string) ($input['password'] ?? '');
        if ($identity === '' || $password === '') {
            respond(['success' => false, 'message' => 'Username/email and password are required'], 422);
        }
        $stmt = $pdo->prepare('SELECT * FROM tblusers WHERE username = ? OR email = ? LIMIT 1');
        $stmt->execute([$identity, strtolower($identity)]);
        $user = $stmt->fetch();
        if (!$user || !password_verify($password, $user['password'])) {
            respond(['success' => false, 'message' => 'Invalid username/email or password'], 401);
        }
        if (!(bool) $user['is_active']) {
            respond(['success' => false, 'message' => 'This account is disabled. Contact an account manager.'], 403);
        }
        $pdo->prepare('UPDATE tblusers SET last_login = NOW() WHERE id = ?')->execute([(int) $user['id']]);
        $user['last_login'] = date('Y-m-d H:i:s');
        respond([
            'success' => true,
            'message' => 'Welcome back',
            'token' => issueToken($pdo, (int) $user['id']),
            'user' => publicUser($user),
        ]);
    }

    if ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === 'me') {
        respond(['success' => true, 'user' => publicUser(requireAuthenticatedUser($pdo))]);
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST' && $action === 'logout') {
        $hash = hash('sha256', bearerToken());
        $pdo->prepare('DELETE FROM api_tokens WHERE token_hash = ?')->execute([$hash]);
        respond(['success' => true, 'message' => 'Signed out']);
    }

    respond(['success' => false, 'message' => 'Unsupported request'], 405);
} catch (PDOException $exception) {
    if ($exception->getCode() === '23000') {
        respond(['success' => false, 'message' => 'Username or email already exists'], 409);
    }
    respond(['success' => false, 'message' => 'Database error'], 500);
}
